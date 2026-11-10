package com.servihome.scgo.registro;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia del registro en un archivo de texto. Un fallo de disco no debe
 * deshacer ni abortar la orden que ya se confirmo en la base: la excepcion se
 * avisa por consola de error y la operacion de negocio sigue.
 */
public class RegistroArchivo implements RegistroOperaciones {

    private final Path archivo;

    public RegistroArchivo(Path archivo) {
        this.archivo = archivo;
    }

    /** Usa -Dscgo.log.path si esta definida; si no, logs/scgo.log. */
    public RegistroArchivo() {
        this(rutaPorDefecto());
    }

    private static Path rutaPorDefecto() {
        String configurada = System.getProperty("scgo.log.path");
        if (configurada != null && !configurada.isBlank()) {
            return Path.of(configurada);
        }
        return Path.of("logs", "scgo.log");
    }

    @Override
    public synchronized void registrar(TipoEvento tipo, String detalle) {
        try {
            Path padre = archivo.getParent();
            if (padre != null) {
                Files.createDirectories(padre);
            }
            String linea = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                    + " | " + tipo + " | " + (detalle == null ? "" : detalle);
            try (BufferedWriter writer = Files.newBufferedWriter(
                    archivo,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {
                writer.write(linea);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println(
                    "Advertencia: no se pudo escribir el registro de operaciones: " + e.getMessage());
        }
    }

    @Override
    public synchronized List<String> leerUltimos(int cantidad) {
        List<String> lineas = new ArrayList<>();
        if (cantidad <= 0 || Files.notExists(archivo)) {
            return lineas;
        }
        try (BufferedReader reader = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                lineas.add(linea);
            }
        } catch (IOException e) {
            System.err.println(
                    "Advertencia: no se pudo leer el registro de operaciones: " + e.getMessage());
            return lineas;
        }
        if (lineas.size() <= cantidad) {
            return lineas;
        }
        return new ArrayList<>(lineas.subList(lineas.size() - cantidad, lineas.size()));
    }
}
