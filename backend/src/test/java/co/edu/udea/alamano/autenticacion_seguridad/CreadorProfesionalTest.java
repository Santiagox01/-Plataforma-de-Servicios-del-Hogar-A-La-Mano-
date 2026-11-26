package co.edu.udea.alamano.autenticacion_seguridad;

import co.edu.udea.alamano.autenticacion_seguridad.aplicacion.CreadorProfesional;
import co.edu.udea.alamano.autenticacion_seguridad.aplicacion.DatosRegistroUsuario;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.EstadoVerificacion;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Profesional;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.RolUsuario;
import co.edu.udea.alamano.autenticacion_seguridad.dominio.Usuario;
import co.edu.udea.alamano.autenticacion_seguridad.infraestructura.IUsuarioRepositorio;
import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.validacion.ValidadorDatosComunes;
import co.edu.udea.alamano.cotizacion_tarificacion.dominio.CategoriaOficio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreadorProfesionalTest {

    @Mock
    private IUsuarioRepositorio usuarioRepositorio;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CreadorProfesional creadorProfesional;

    @BeforeEach
    void setUp() {
        creadorProfesional = new CreadorProfesional(usuarioRepositorio, passwordEncoder, new ValidadorDatosComunes());
    }

    @Test
    @DisplayName("PU-03: Registro de profesional nace con Pendiente de verificación y disponible=false")
    void debeCrearProfesionalPendienteYNoDisponible() {
        CategoriaOficio electricista = new CategoriaOficio("Electricidad", "Instalaciones eléctricas");
        DatosRegistroUsuario datos = new DatosRegistroUsuario(
                "Mateo Vargas",
                "mateo@alamano.co",
                "3109876543",
                "ClaveSegura1",
                "Laureles, Medellín",
                6.2447,
                -75.5950,
                "identidades/cedula-mateo.pdf",
                List.of("certificaciones/conte.pdf"),
                List.of(electricista)
        );

        when(usuarioRepositorio.existePorCorreo("mateo@alamano.co")).thenReturn(false);
        when(passwordEncoder.encode("ClaveSegura1")).thenReturn("$2a$10$hashSimulado");
        when(usuarioRepositorio.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario creado = creadorProfesional.registrar(datos);

        assertThat(creado).isInstanceOf(Profesional.class);
        Profesional profesional = (Profesional) creado;
        assertThat(profesional.getRol()).isEqualTo(RolUsuario.PROFESIONAL);
        assertThat(profesional.getEstadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE_VERIFICACION);
        assertThat(profesional.isDisponible()).isFalse();
        assertThat(profesional.getServicios()).hasSize(1);
        verify(usuarioRepositorio).guardar(any(Profesional.class));

        assertThatThrownBy(() -> profesional.establecerDisponibilidad(true))
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessageContaining("verificado");
    }
}
