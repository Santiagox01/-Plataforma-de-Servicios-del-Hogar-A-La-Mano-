package co.edu.udea.alamano.gestion_servicios.infraestructura;

import co.edu.udea.alamano.gestion_servicios.dominio.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MensajeRepositorio extends JpaRepository<Mensaje, Long> {

    @Query("""
            SELECT m FROM Mensaje m
            JOIN FETCH m.remitente
            WHERE m.chat.idChat = :idChat
            ORDER BY m.fechaEnvio ASC, m.idMensaje ASC
            """)
    List<Mensaje> listarPorChat(@Param("idChat") Long idChat);

    @Query("""
            SELECT m FROM Mensaje m
            JOIN FETCH m.remitente
            JOIN FETCH m.chat c
            WHERE m.idMensaje = :idMensaje AND c.solicitud.idSolicitud = :idSolicitud
            """)
    Optional<Mensaje> buscarPorIdYSolicitud(@Param("idMensaje") Long idMensaje, @Param("idSolicitud") Long idSolicitud);
}
