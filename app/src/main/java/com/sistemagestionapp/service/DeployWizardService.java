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

/**
 * En este servicio centralizo la lógica principal del asistente de despliegue.
 * Me encargo de gestionar los pasos, su estado y de decidir cuándo el proceso
 * completo puede darse por finalizado correctamente.
 */
@Service
public class DeployWizardService {

    // Repositorio de aplicaciones para obtener la app asociada a cada asistente
    private final AplicacionRepository aplicacionRepository;

    // Repositorio que utilizo para guardar y consultar el estado de cada paso
    private final ControlDespliegueRepository controlDespliegueRepository;

    /**
     * Defino aquí los pasos “reales” del asistente.
     * Excluyo el RESUMEN_FINAL porque no es un paso que el usuario ejecute,
     * sino un estado calculado automáticamente.
     */
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

    /**
     * Obtengo todos los controles de una aplicación ordenados por paso.
     * Este método lo utilizo para mostrar el resumen del asistente.
     */
    @Transactional(readOnly = true)
    public List<ControlDespliegue> obtenerControlesOrdenados(Long aplicacionId) {
        return controlDespliegueRepository.findByAplicacionIdOrderByPasoAsc(aplicacionId);
    }

    /**
     * Marco un paso concreto con el estado indicado (OK, KO o PENDIENTE).
     * Si el control no existe aún, lo creo.
     * Cada vez que se marca un paso, compruebo si el resumen final debe actualizarse.
     */
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

        // Tras marcar un paso, compruebo si ya se puede dar por completado el resumen final
        actualizarResumenFinalSiProcede(aplicacionId);
    }

    /**
     * Obtengo el control de un paso concreto de una aplicación.
     * Este método lo utilizo principalmente desde los controladores del asistente.
     */
    @Transactional(readOnly = true)
    public Optional<ControlDespliegue> obtenerControl(Long aplicacionId, PasoDespliegue paso) {
        return controlDespliegueRepository.findByAplicacionIdAndPaso(aplicacionId, paso);
    }

    /**
     * Cuento cuántos pasos están en estado OK para una aplicación.
     * Me sirve para calcular el progreso global del asistente.
     */
    @Transactional(readOnly = true)
    public long contarPasosOk(Long aplicacionId) {
        return controlDespliegueRepository.countByAplicacionIdAndEstado(aplicacionId, EstadoControl.OK);
    }

    /**
     * Devuelvo el número total de pasos definidos en el enum.
     * Este valor se utiliza para calcular porcentajes de progreso.
     */
    public int totalPasos() {
        return PasoDespliegue.values().length;
    }

    /**
     * Compruebo si todos los pasos reales del asistente están en estado OK.
     * Si es así, marco automáticamente el paso RESUMEN_FINAL como OK.
     * Este proceso es idempotente, por lo que puede ejecutarse varias veces sin efectos secundarios.
     */
    private void actualizarResumenFinalSiProcede(Long aplicacionId) {

        boolean todosOk = PASOS_REALES.stream().allMatch(p ->
                controlDespliegueRepository.findByAplicacionIdAndPaso(aplicacionId, p)
                        .map(c -> c.getEstado() == EstadoControl.OK)
                        .orElse(false)
        );

        if (todosOk) {
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
            // Si no están todos los pasos en OK, no modifico el resumen final.
            // Podría marcarlo como PENDIENTE o KO, pero para este proyecto lo dejo así.
        }
    }
}
