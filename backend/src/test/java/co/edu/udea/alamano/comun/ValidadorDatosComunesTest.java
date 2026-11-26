package co.edu.udea.alamano.comun;

import co.edu.udea.alamano.comun.excepcion.ExcepcionNegocio;
import co.edu.udea.alamano.comun.validacion.ValidadorDatosComunes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorDatosComunesTest {

    private final ValidadorDatosComunes validador = new ValidadorDatosComunes();

    @Test
    @DisplayName("Valida correo, celular colombiano de 10 dígitos y política de contraseña")
    void validaDatosBasicosCorrectamente() {
        assertThat(validador.normalizarYValidarCorreo("  Carlos@Correo.CO "))
                .isEqualTo("carlos@correo.co");
        assertThat(validador.validarCelularColombiano("3001234567"))
                .isEqualTo("3001234567");
        assertThatCode(() -> validador.validarContrasena("Segura123"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> validador.validarCelularColombiano("6041234567"))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> validador.validarCelularColombiano("30012345"))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> validador.validarContrasena("minuscula1"))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> validador.validarContrasena("SinNumero"))
                .isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    @DisplayName("Valida formato permitido (JPG/PNG/PDF) y tamaño máximo de 10 MB")
    void validaArchivosDocumento() {
        MockMultipartFile pdfValido = new MockMultipartFile(
                "documentoIdentidad", "cedula.pdf", "application/pdf", new byte[1024]
        );
        assertThatCode(() -> validador.validarArchivoDocumento(pdfValido)).doesNotThrowAnyException();

        MockMultipartFile ejecutable = new MockMultipartFile(
                "documentoIdentidad", "virus.exe", "application/octet-stream", new byte[100]
        );
        assertThatThrownBy(() -> validador.validarArchivoDocumento(ejecutable))
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessageContaining("JPG, PNG o PDF");

        byte[] muyGrande = new byte[(int) ValidadorDatosComunes.TAMANO_MAXIMO_ARCHIVO_BYTES + 1];
        MockMultipartFile pdfGigante = new MockMultipartFile(
                "documentoIdentidad", "grande.pdf", "application/pdf", muyGrande
        );
        assertThatThrownBy(() -> validador.validarArchivoDocumento(pdfGigante))
                .isInstanceOf(ExcepcionNegocio.class)
                .hasMessageContaining("10 MB");
    }
}
