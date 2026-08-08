package ec.edu.espol.paipay.datalogger.domain;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.MovimientoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.ObservacionPezLocal;
import ec.edu.espol.paipay.datalogger.data.local.model.JornadaConPeces;
import ec.edu.espol.paipay.datalogger.data.remote.dto.JornadaApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.MovimientoApiDto;

public class ComparadorConflictosTest {
    @Test public void jornadaMuestraAmbasVersionesYDiferenciasImportantes() {
        JornadaConPeces local = new JornadaConPeces();
        local.jornada = new JornadaLocal();
        local.jornada.versionServidor = 1;
        local.jornada.piscinaCodigo = "P-01";
        local.jornada.capturadaEn = "2026-08-07T20:00:00Z";
        local.jornada.poblacionEstimada = 187;
        local.jornada.observaciones = "Cambio local";
        local.jornada.incluyeAgua = true;
        local.jornada.ph = 7.2;
        local.jornada.nitrato = 4.0;
        local.jornada.nitrito = 0.1;
        local.jornada.amoniacoTotal = 0.25;
        ObservacionPezLocal pez = new ObservacionPezLocal();
        pez.pesoGramos = 250;
        pez.tallaCentimetros = 21;
        local.peces = Collections.singletonList(pez);

        JornadaApiDto remota = new JornadaApiDto();
        remota.version = 2;
        remota.piscinaCodigo = "P-01";
        remota.capturadaEn = local.jornada.capturadaEn;
        remota.poblacionEstimada = 190;
        remota.observaciones = "Cambio web";
        remota.agua = new JornadaApiDto.AguaDto();
        remota.agua.ph = "7.4";
        remota.agua.nitrato = "4";
        remota.agua.nitrito = "0.1";
        remota.agua.amoniacoTotal = "0.25";
        remota.peces = new ArrayList<>();

        String resultado = ComparadorConflictos.jornada(local, remota);

        assertTrue(resultado.contains("TELÉFONO (basado en v1)"));
        assertTrue(resultado.contains("SERVIDOR (v2)"));
        assertTrue(resultado.contains("Población estimada: teléfono «187» / servidor «190»"));
        assertTrue(resultado.contains("Biometría: teléfono «1 pez/peces"));
    }

    @Test public void movimientoComparaCantidadYDestino() {
        MovimientoLocal local = new MovimientoLocal();
        local.versionServidor = 3;
        local.tipo = "TRASLADO";
        local.cantidad = 13;
        local.piscinaOrigenUuid = "p-1";
        local.piscinaDestinoUuid = "p-2";
        local.ocurridoEn = "2026-08-07T20:00:00Z";

        MovimientoApiDto remoto = new MovimientoApiDto();
        remoto.version = 4;
        remoto.tipo = "TRASLADO";
        remoto.cantidad = 10;
        remoto.piscinaOrigen = "p-1";
        remoto.piscinaDestino = "p-3";
        remoto.ocurridoEn = local.ocurridoEn;

        String resultado = ComparadorConflictos.movimiento(local, remoto);

        assertTrue(resultado.contains("Cantidad: teléfono «13» / servidor «10»"));
        assertTrue(resultado.contains("Piscina de destino: teléfono «p-2» / servidor «p-3»"));
    }

    @Test public void cambioDeVersionSinCamposVisiblesSigueProtegido() {
        MovimientoLocal local = new MovimientoLocal();
        local.versionServidor = 1;
        local.tipo = "MORTALIDAD";
        local.cantidad = 2;
        local.ocurridoEn = "2026-08-07T20:00:00Z";

        MovimientoApiDto remoto = new MovimientoApiDto();
        remoto.version = 2;
        remoto.tipo = local.tipo;
        remoto.cantidad = local.cantidad;
        remoto.ocurridoEn = local.ocurridoEn;

        String resultado = ComparadorConflictos.movimiento(local, remoto);

        assertTrue(resultado.contains("No hay diferencias visibles"));
        assertTrue(resultado.contains("SERVIDOR (v2)"));
    }
}
