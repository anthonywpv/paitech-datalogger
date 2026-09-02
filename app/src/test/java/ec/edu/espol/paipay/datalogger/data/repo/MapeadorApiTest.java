package ec.edu.espol.paipay.datalogger.data.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;

import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.CicloLombriculturaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.MovimientoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroLombriculturaLocal;
import ec.edu.espol.paipay.datalogger.data.local.model.JornadaConPeces;
import ec.edu.espol.paipay.datalogger.data.remote.dto.JornadaApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.MovimientoApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.RegistroLombriculturaApiDto;

public class MapeadorApiTest {

    @Test
    public void creacionDeJornadaOmiteVersionCeroYEdicionEnviaVersionReal() {
        JornadaConPeces local = new JornadaConPeces();
        local.jornada = new JornadaLocal();
        local.peces = new ArrayList<>();
        local.jornada.versionServidor = 0;

        JornadaApiDto creacion = MapeadorApi.aDto(local, "avd-prueba");
        assertNull(creacion.version);

        local.jornada.versionServidor = 3;
        JornadaApiDto edicion = MapeadorApi.aDto(local, "avd-prueba");
        assertEquals(Integer.valueOf(3), edicion.version);
    }

    @Test
    public void creacionDeMovimientoOmiteVersionCeroYEdicionEnviaVersionReal() {
        MovimientoLocal local = new MovimientoLocal();
        local.versionServidor = 0;

        MovimientoApiDto creacion = MapeadorApi.aDto(local);
        assertNull(creacion.version);

        local.versionServidor = 2;
        MovimientoApiDto edicion = MapeadorApi.aDto(local);
        assertEquals(Integer.valueOf(2), edicion.version);
    }

    @Test
    public void movimientoRemotoConservaSuAutorComunitario() {
        MovimientoApiDto remoto = new MovimientoApiDto();
        remoto.id = "mov-1";
        remoto.tipo = "MORTALIDAD";
        remoto.autor = new JornadaApiDto.AutorDto();
        remoto.autor.correo = "otra.persona@paipayales.test";

        MovimientoLocal local = MapeadorApi.aLocal(remoto, "sesion@paipayales.test");

        assertEquals("otra.persona@paipayales.test", local.autorCorreo);
    }

    @Test
    public void registroLombriculturaConservaAutorYVersionaSoloCorrecciones() {
        RegistroLombriculturaLocal local = new RegistroLombriculturaLocal();
        local.uuid = "registro-1";
        local.camaUuid = "cama-1";
        local.cicloUuid = "ciclo-1";
        local.capturadaEn = "2026-09-01T18:00:00Z";
        local.phSuelo = 7.35d;
        local.conteoLombrices = 180;

        RegistroLombriculturaApiDto creacion = MapeadorApi.aDto(local, "avd-prueba");
        assertNull(creacion.version);
        assertEquals("7.35", creacion.phSuelo);

        local.versionServidor = 4;
        RegistroLombriculturaApiDto edicion = MapeadorApi.aDto(local, "avd-prueba");
        assertEquals(Integer.valueOf(4), edicion.version);

        edicion.autor = new JornadaApiDto.AutorDto();
        edicion.autor.correo = "otra.persona@paipayales.test";
        edicion.estado = "COMPLETO";
        RegistroLombriculturaLocal descargado = MapeadorApi.aLocal(
                edicion, "sesion@paipayales.test");
        assertEquals("otra.persona@paipayales.test", descargado.autorCorreo);
        assertEquals(180, descargado.conteoLombrices);
    }

    @Test
    public void cierreLombriculturaEnviaVersionConocidaYConteoFinal() {
        CicloLombriculturaLocal local = new CicloLombriculturaLocal();
        local.versionServidor = 2;
        local.cerradoEn = "2027-03-01T18:00:00Z";
        local.conteoFinal = 275;

        assertEquals(2, MapeadorApi.cierreDto(local).version);
        assertEquals(275, MapeadorApi.cierreDto(local).conteoFinal);
    }
}
