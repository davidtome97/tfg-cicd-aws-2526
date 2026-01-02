package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Aplicacion;
import com.sistemagestionapp.model.ControlDespliegue;
import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import com.sistemagestionapp.repository.AplicacionRepository;
import com.sistemagestionapp.repository.ControlDespliegueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
public class DeployWizardService {

    private final AplicacionRepository aplicacionRepository;
    private final ControlDespliegueRepository controlDespliegueRepository;

    // Los 6 pasos “reales” (sin incluir RESUMEN_FINAL)
    private static final EnumSet<PasoDespliegue> PASOS_REALES = EnumSet.of(
            PasoDespliegue.SONAR_ANALISIS,
            PasoDespliegue.SONAR_INTEGRACION_GIT,
            PasoDespliegue.REPOSITORIO_GIT,
            PasoDespliegue.IMAGEN_ECR,
            PasoDespliegue.DESPLIEGUE_EC2,
            PasoDespliegue.BASE_DATOS
    );

    public DeployWizardService(AplicacionRepository aplicacionRepository,
                               ControlDespliegueRepository controlDespliegueRepository) {
        this.aplicacionRepository = aplicacionRepository;
        this.controlDespliegueRepository = controlDespliegueRepository;
    }

    @Transactional(readOnly = true)
    public List<ControlDespliegue> obtenerControlesOrdenados(Long aplicacionId) {
        return controlDespliegueRepository.findByAplicacionIdOrderByPasoAsc(aplicacionId);
    }

    @Transactional
    public void marcarPaso(Long aplicacionId, PasoDespliegue paso, EstadoControl estado, String mensaje) {
        Aplicacion app = aplicacionRepository.findById(aplicacionId)
                .orElseThrow(() -> new IllegalArgumentException("Aplicación no encontrada: " + aplicacionId));

        ControlDespliegue control = controlDespliegueRepository
                .findByAplicacionIdAndPaso(aplicacionId, paso)
                .orElseGet(() -> {
                    ControlDespliegue cd = new ControlDespliegue();
                    cd.setAplicacion(app);
                    cd.setPaso(paso);
                    cd.setEstado(EstadoControl.PENDIENTE);
                    return cd;
                });

        control.setEstado(estado);
        control.setMensaje(mensaje);
        control.setFechaEjecucion(LocalDateTime.now());

        controlDespliegueRepository.save(control);

        // ✅ auto-marca RESUMEN_FINAL cuando proceda
        actualizarResumenFinalSiProcede(aplicacionId);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ControlDespliegue> obtenerControl(Long aplicacionId, PasoDespliegue paso) {
        return controlDespliegueRepository.findByAplicacionIdAndPaso(aplicacionId, paso);
    }

    @Transactional(readOnly = true)
    public long contarPasosOk(Long aplicacionId) {
        return controlDespliegueRepository.countByAplicacionIdAndEstado(aplicacionId, EstadoControl.OK);
    }

    public int totalPasos() {
        return PasoDespliegue.values().length; // 6
    }

    private void actualizarResumenFinalSiProcede(Long aplicacionId) {
        // 1) Si alguno de los pasos reales no está OK -> RESUMEN_FINAL = KO/PENDIENTE según prefieras
        boolean todosOk = PASOS_REALES.stream().allMatch(p ->
                controlDespliegueRepository.findByAplicacionIdAndPaso(aplicacionId, p)
                        .map(c -> c.getEstado() == EstadoControl.OK)
                        .orElse(false)
        );

        if (todosOk) {
            // Marcar RESUMEN_FINAL como OK (idempotente)
            ControlDespliegue resumen = controlDespliegueRepository
                    .findByAplicacionIdAndPaso(aplicacionId, PasoDespliegue.RESUMEN_FINAL)
                    .orElseGet(() -> {
                        Aplicacion app = aplicacionRepository.findById(aplicacionId)
                                .orElseThrow(() -> new IllegalArgumentException("Aplicación no encontrada: " + aplicacionId));
                        ControlDespliegue cd = new ControlDespliegue();
                        cd.setAplicacion(app);
                        cd.setPaso(PasoDespliegue.RESUMEN_FINAL);
                        cd.setEstado(EstadoControl.PENDIENTE);
                        return cd;
                    });

            resumen.setEstado(EstadoControl.OK);
            resumen.setMensaje("Todos los pasos completados correctamente.");
            resumen.setFechaEjecucion(LocalDateTime.now());
            controlDespliegueRepository.save(resumen);
        } else {
            // Si no están todos OK, puedes:
            // A) no tocar RESUMEN_FINAL (lo dejo así por defecto)
            // B) o marcarlo como PENDIENTE/KO. Si lo quieres, dímelo y lo activamos.
        }
    }
}
