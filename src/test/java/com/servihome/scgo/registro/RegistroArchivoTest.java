package com.servihome.scgo.registro;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistroArchivoTest {

    @TempDir
    Path tempDir;

    @Test
    void escribeTresEventosYLeeLosUltimosDosEnOrden() {
        RegistroArchivo registro = new RegistroArchivo(tempDir.resolve("scgo.log"));
        registro.registrar(TipoEvento.ORDEN_GENERADA, "orden=1");
        registro.registrar(TipoEvento.RECURSO_ASIGNADO, "orden=1 recurso=2");
        registro.registrar(TipoEvento.ORDEN_CERRADA, "orden=1 horas=4.00");

        List<String> ultimos = registro.leerUltimos(2);

        assertEquals(2, ultimos.size());
        assertTrue(ultimos.get(0).contains("| RECURSO_ASIGNADO |"));
        assertTrue(ultimos.get(1).matches(
                "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2} \\| ORDEN_CERRADA \\| orden=1 horas=4.00"));
        assertEquals(3, registro.leerUltimos(10).size());
    }

    @Test
    void archivoInexistenteDevuelveListaVacia() {
        RegistroArchivo registro = new RegistroArchivo(tempDir.resolve("no-existe.log"));
        assertTrue(registro.leerUltimos(10).isEmpty());
    }
}
