package com.sistemagestionapp.repository;

import com.sistemagestionapp.model.ControlDespliegue;
import com.sistemagestionapp.model.EstadoControl;
import com.sistemagestionapp.model.PasoDespliegue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * En este repositorio gestiono el acceso a datos de la entidad {@link ControlDespliegue}.
 *
 * Utilizo consultas derivadas de Spring Data JPA para recuperar el estado de los pasos
 * del asistente, calcular el progreso de una aplicación y obtener información del
 * último paso ejecutado.
 *
 * @author David Tomé Arnaiz
 */
public interface ControlDespliegueRepository extends JpaRepository<ControlDespliegue, Long> {

    /**
     * Obtengo todos los controles de despliegue de una aplicación ordenados por paso.
     *
     * Este método se utiliza para construir el resumen del asistente y mostrar
     * el estado de cada paso en la interfaz.
     *
     * @param aplicacionId identificador de la aplicación
     * @return lista ordenada de controles de despliegue
     */
    List<ControlDespliegue> findByAplicacionIdOrderByPasoAsc(Long aplicacionId);

    /**
     * Obtengo el control asociado a un paso concreto de una aplicación.
     *
     * Este método permite actualizar un paso de forma idempotente,
     * evitando duplicados.
     *
     * @param aplicacionId identificador de la aplicación
     * @param paso paso del asistente
     * @return control del paso si existe
     */
    Optional<ControlDespliegue> findByAplicacionIdAndPaso(Long aplicacionId, PasoDespliegue paso);

    /**
     * Cuento el número de pasos de una aplicación que se encuentran en un estado concreto.
     *
     * Este método se utiliza para calcular el progreso general del asistente.
     *
     * @param aplicacionId identificador de la aplicación
     * @param estado estado a contabilizar
     * @return número de pasos en dicho estado
     */
    long countByAplicacionIdAndEstado(Long aplicacionId, EstadoControl estado);

    /**
     * Cuento el número de pasos pertenecientes a un conjunto concreto que se encuentran en un estado determinado.
     *
     * Este método se utiliza para calcular el progreso excluyendo pasos no relevantes
     * como el resumen final.
     *
     * @param aplicacionId identificador de la aplicación
     * @param pasos conjunto de pasos a considerar
     * @param estado estado a contabilizar
     * @return número de pasos en dicho estado
     */
    long countByAplicacionIdAndPasoInAndEstado(Long aplicacionId,
                                               Set<PasoDespliegue> pasos,
                                               EstadoControl estado);

    /**
     * Obtengo el último control de despliegue ejecutado para una aplicación.
     *
     * Este método permite conocer el paso más recientemente actualizado.
     *
     * @param aplicacionId identificador de la aplicación
     * @return último control de despliegue ejecutado, si existe
     */
    Optional<ControlDespliegue> findTopByAplicacionIdOrderByFechaEjecucionDesc(Long aplicacionId);
}
