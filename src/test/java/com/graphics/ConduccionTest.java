package com.graphics;

import java.util.ArrayDeque;
import java.util.List;

import com.graphics.juego.ciudad.MapaCiudad;
import com.graphics.juego.conduccion.Auto;
import com.graphics.juego.conduccion.Chata;
import com.graphics.juego.conduccion.ControlesAuto;
import com.graphics.juego.conduccion.Obstaculos;
import com.graphics.juego.funciones.Entregas;
import com.graphics.juego.funciones.Semaforos;
import com.graphics.juego.trafico.Trafico;
import com.graphics.juego.trafico.VehiculoAutonomo;

import junit.framework.TestCase;

/** Comprueba las reglas de la ciudad y de la conduccion sin abrir una ventana OpenGL. */
public class ConduccionTest extends TestCase {

    private final MapaCiudad mapa = new MapaCiudad();

    /** Requisitos del enunciado: 11x11, 12 manzanas con edificios, 4 parques, 9 farolas. */
    public void testMapaAmpliado() {
        assertTrue(mapa.filas() >= 11);
        assertTrue(mapa.columnas() >= 11);
        assertEquals(10f, MapaCiudad.CELDA, 0f); // se conserva el tamano de celda
        assertTrue(mapa.contar('E') >= 12);
        assertTrue(mapa.contar('P') >= 4);
        assertTrue(mapa.farolas().size() >= 9);
        assertEquals(55f, mapa.limite(), 0f); // los limites salen del tamano de la matriz
    }

    /** Todos los destinos y la salida estan conectados por calles (busqueda en anchura). */
    public void testDestinosAlcanzablesPorCalles() {
        boolean[][] visitada = new boolean[mapa.filas()][mapa.columnas()];
        ArrayDeque<int[]> cola = new ArrayDeque<>();
        float[] salida = mapa.salida();
        cola.add(new int[] { mapa.filaDe(salida[1]), mapa.columnaDe(salida[0]) });
        while (!cola.isEmpty()) {
            int[] c = cola.poll();
            if (!mapa.esCalle(c[0], c[1]) || visitada[c[0]][c[1]]) {
                continue;
            }
            visitada[c[0]][c[1]] = true;
            for (int d = 0; d < 4; d++) {
                cola.add(new int[] { c[0] + MapaCiudad.DF[d], c[1] + MapaCiudad.DC[d] });
            }
        }
        List<float[]> destinos = mapa.destinos();
        assertTrue(destinos.size() >= 3);
        for (float[] d : destinos) {
            assertTrue(visitada[mapa.filaDe(d[1])][mapa.columnaDe(d[0])]);
            assertTrue(mapa.puedeCircular(d[0], d[1], Auto.RADIO));
        }
    }

    public void testCallesNuevasTransitables() {
        for (float z = -50; z <= 50; z += 0.25f) {
            assertTrue(mapa.puedeCircular(50, z, Auto.RADIO));  // calle del borde este (nueva)
            assertTrue(mapa.puedeCircular(-50, z, Auto.RADIO)); // calle del borde oeste
        }
        for (float x = -50; x <= 50; x += 0.25f) {
            assertTrue(mapa.puedeCircular(x, 50, Auto.RADIO));  // avenida sur (nueva)
            assertTrue(mapa.puedeCircular(x, 30, Auto.RADIO));  // avenida interior nueva
        }
    }

    public void testObstaculosYLimites() {
        assertFalse(mapa.puedeCircular(-40, -40, Auto.RADIO)); // edificio
        assertFalse(mapa.puedeCircular(40, 20, Auto.RADIO));   // parque del sector sureste
        // La manzana de la columna 1 llega hasta x = -35: a 1 unidad el circulo la invade
        assertFalse(mapa.puedeCircular(-34, -40, Auto.RADIO));
        assertTrue(mapa.puedeCircular(-33, -40, Auto.RADIO));
        assertFalse(mapa.puedeCircular(54, 0, Auto.RADIO));    // demasiado cerca del borde este
        assertFalse(mapa.puedeCircular(0, -56, Auto.RADIO));   // fuera del mapa
    }

    /** Conducir contra un edificio nunca deja al auto dentro de una manzana. */
    public void testChoqueNoAtraviesa() {
        Auto auto = new Auto(-30, -30, (float) Math.PI); // cruce, mirando al sur hacia la calle
        auto.angulo = (float) (Math.PI * 0.75); // apunta en diagonal hacia una manzana
        for (int i = 0; i < 600; i++) {
            auto.actualizar(1 / 60f, new ControlesAuto(1, 0, false),
                    (xa, za, x, z) -> mapa.puedeCircular(x, z, Auto.RADIO));
            assertTrue(mapa.puedeCircular(auto.x, auto.z, Auto.RADIO));
        }
    }

    /** El mismo recorrido a 30 FPS y a 144 FPS termina practicamente en el mismo lugar. */
    public void testMovimientoIndependienteDeLosFps() {
        float[] lento = simular(1 / 30f);
        float[] rapido = simular(1 / 144f);
        assertEquals(lento[0], rapido[0], 0.6f);
        assertEquals(lento[1], rapido[1], 0.6f);
        assertEquals(lento[2], rapido[2], 0.2f);
    }

    private float[] simular(float dt) {
        Auto auto = new Auto(0, 0, 0);
        int pasos = Math.round(3 / dt);
        for (int i = 0; i < pasos; i++) {
            float giro = i < pasos / 2 ? 0 : 1;
            auto.actualizar(dt, new ControlesAuto(1, giro, false), (xa, za, x, z) -> true);
        }
        return new float[] { auto.x, auto.z, auto.velocidad };
    }

    /** 5 minutos de trafico: nadie pisa una manzana, nadie sale del mapa y todos giran. */
    public void testTraficoSigueLasCalles() {
        Trafico trafico = new Trafico(mapa, 5);
        Semaforos semaforos = new Semaforos(mapa);
        Auto jugador = new Auto(1000, 1000, 0); // lejos, para no estorbar
        assertTrue(trafico.vehiculos().size() >= 3);
        int n = trafico.vehiculos().size();
        float[] anguloInicial = new float[n];
        boolean[] giro = new boolean[n];
        float[] distanciaRecorrida = new float[n];
        for (int i = 0; i < n; i++) {
            anguloInicial[i] = trafico.vehiculos().get(i).angulo;
        }
        float dt = 1 / 60f;
        for (int paso = 0; paso < 60 * 300; paso++) {
            semaforos.actualizar(dt);
            trafico.actualizar(dt, semaforos, jugador);
            for (int i = 0; i < n; i++) {
                VehiculoAutonomo v = trafico.vehiculos().get(i);
                // 0.9 = media anchura del auto: la carroceria nunca toca una acera
                assertTrue("vehiculo " + i + " fuera de la calle en " + v.x + "," + v.z,
                        mapa.puedeCircular(v.x, v.z, 0.9f));
                assertTrue(v.velocidad <= VehiculoAutonomo.VELOCIDAD_RECTA + 0.001f);
                distanciaRecorrida[i] += v.velocidad * dt;
                float diferencia = Math.abs((float) Math.IEEEremainder(v.angulo - anguloInicial[i], Math.PI * 2));
                if (diferencia > 1.4f) {
                    giro[i] = true;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            assertTrue("el vehiculo " + i + " no giro", giro[i]);
            assertTrue("el vehiculo " + i + " casi no se movio", distanciaRecorrida[i] > 500);
        }
    }

    /** Secuencia del semaforo: verde -> amarillo -> rojo -> verde... y ejes opuestos. */
    public void testSemaforoSecuencia() {
        assertEquals(Semaforos.Luz.VERDE, Semaforos.luzEn(0));
        assertEquals(Semaforos.Luz.AMARILLO, Semaforos.luzEn(Semaforos.VERDE + 0.5f));
        assertEquals(Semaforos.Luz.ROJO, Semaforos.luzEn(Semaforos.VERDE + Semaforos.AMARILLO + 0.5f));
        assertEquals(Semaforos.Luz.VERDE, Semaforos.luzEn(Semaforos.CICLO + 0.5f));
        Semaforos s = new Semaforos(mapa);
        for (int i = 0; i < 2000; i++) {
            s.actualizar(0.013f);
            // Nunca estan en verde los dos ejes del mismo cruce
            assertFalse(s.estado(2, 2, true) == Semaforos.Luz.VERDE && s.estado(2, 2, false) == Semaforos.Luz.VERDE);
        }
    }

    /** Completar todas las entregas y volver a empezar con el mismo reinicio de la tecla R. */
    public void testEntregasCompletasYReinicio() {
        Entregas entregas = new Entregas(mapa);
        float[] salida = mapa.salida();
        Auto auto = new Auto(salida[0], salida[1], salida[2]);
        for (int vuelta = 0; vuelta < 2; vuelta++) {
            assertEquals(0, entregas.completadas());
            for (float[] d : mapa.destinos()) {
                auto.x = d[0] + 10; // lejos: no cuenta
                auto.z = d[1];
                entregas.actualizar(0.1f, auto);
                auto.x = d[0];
                auto.velocidad = 5; // llega pero sin frenar: no cuenta
                entregas.actualizar(0.1f, auto);
                auto.velocidad = 0; // llega y frena: cuenta
                entregas.actualizar(0.1f, auto);
            }
            assertTrue(entregas.terminado());
            assertEquals(entregas.total(), entregas.completadas());
            // Tecla R
            auto.reiniciar();
            entregas.reiniciar();
            assertEquals(0f, entregas.tiempo(), 0f);
        }
    }

    public void testReinicio() {
        float[] salida = mapa.salida();
        Auto auto = new Auto(salida[0], salida[1], salida[2]);
        auto.x = 12;
        auto.z = 4;
        auto.velocidad = 10;
        auto.angulo = 2;
        auto.reiniciar();
        assertEquals(salida[0], auto.x, 0f);
        assertEquals(salida[1], auto.z, 0f);
        assertEquals(0f, auto.velocidad, 0f);
        assertEquals(salida[2], auto.angulo, 0f);
    }

    // ===================== Chata (remolque) =====================

    private static final Obstaculos SIN_OBSTACULOS = (xa, za, x, z) -> true;

    /** Avanza el conjunto auto + chata "segundos" segundos con los mismos controles. */
    private static void conducir(Auto auto, Chata chata, float dt, float segundos, ControlesAuto c,
            Obstaculos paraAuto, Obstaculos paraChata) {
        int pasos = Math.round(segundos / dt);
        for (int i = 0; i < pasos; i++) {
            chata.conducir(dt, auto, c, paraAuto, paraChata);
        }
    }

    /** La lanza siempre queda pegada al enganche: el eje esta a LANZA del enganche. */
    private static void assertLanzaPegada(Auto auto, Chata chata) {
        float[] p = Chata.enganche(auto.x, auto.z, auto.angulo);
        assertEquals(Chata.LANZA, (float) Math.hypot(chata.ejeX - p[0], chata.ejeZ - p[1]), 1e-3f);
    }

    /** En recta la chata queda alineada justo detras del auto; detenida no gira. */
    public void testChataSeAlineaEnRecta() {
        Auto auto = new Auto(0, 0, 0);
        Chata chata = new Chata(auto);
        float dt = 1 / 60f;
        // Detenido: la chata no gira
        conducir(auto, chata, dt, 2, ControlesAuto.NINGUNO, SIN_OBSTACULOS, SIN_OBSTACULOS);
        assertEquals(0f, chata.angulo, 1e-6f);
        // Curva para desalinearla y despues recta
        conducir(auto, chata, dt, 1.5f, new ControlesAuto(1, 1, false), SIN_OBSTACULOS, SIN_OBSTACULOS);
        assertTrue(Math.abs(chata.anguloRelativo(auto)) > 0.1f);
        conducir(auto, chata, dt, 6, new ControlesAuto(1, 0, false), SIN_OBSTACULOS, SIN_OBSTACULOS);
        assertEquals(0f, chata.anguloRelativo(auto), 0.01f);
        // El eje queda detras del auto a ENGANCHE + LANZA
        float distancia = Chata.ENGANCHE + Chata.LANZA;
        assertEquals(auto.x - auto.frenteX() * distancia, chata.ejeX, 0.05f);
        assertEquals(auto.z - auto.frenteZ() * distancia, chata.ejeZ, 0.05f);
        assertLanzaPegada(auto, chata);
    }

    /** Al girar la chata se queda atras (se abre hacia afuera) y luego converge. */
    public void testChataSeQuedaAtrasAlGirarYConverge() {
        Auto auto = new Auto(0, 0, 0);
        Chata chata = new Chata(auto);
        float dt = 1 / 60f;
        conducir(auto, chata, dt, 1, new ControlesAuto(1, 0, false), SIN_OBSTACULOS, SIN_OBSTACULOS);
        float maximo = 0;
        for (int i = 0; i < 90; i++) { // 1.5 s girando a la izquierda (el angulo del auto aumenta)
            chata.conducir(dt, auto, new ControlesAuto(1, 1, false), SIN_OBSTACULOS, SIN_OBSTACULOS);
            float relativo = chata.anguloRelativo(auto);
            assertTrue("la chata debe ir detras del giro del auto", relativo >= 0);
            maximo = Math.max(maximo, relativo);
            assertLanzaPegada(auto, chata);
        }
        assertTrue("la chata casi no se abrio: " + maximo, maximo > 0.15f);
        conducir(auto, chata, dt, 8, new ControlesAuto(1, 0, false), SIN_OBSTACULOS, SIN_OBSTACULOS);
        assertEquals(0f, chata.anguloRelativo(auto), 0.01f);
    }

    /** El angulo relativo nunca supera el limite, tampoco en reversa (acuchillamiento). */
    public void testAnguloRelativoLimitado() {
        Auto auto = new Auto(0, 0, 0);
        Chata chata = new Chata(auto);
        float dt = 1 / 60f;
        float limite = Chata.ANGULO_MAXIMO + 1e-4f;
        float maximoReversa = 0;
        ControlesAuto[] maniobras = {
                new ControlesAuto(1, 1, false),   // adelante girando
                new ControlesAuto(-1, 1, false),  // reversa girando
                new ControlesAuto(-1, -1, false), // reversa al otro lado
                new ControlesAuto(-1, 0, false)   // reversa recta: el remolque se cierra solo
        };
        for (ControlesAuto c : maniobras) {
            for (int i = 0; i < 60 * 6; i++) {
                chata.conducir(dt, auto, c, SIN_OBSTACULOS, SIN_OBSTACULOS);
                float relativo = Math.abs(chata.anguloRelativo(auto));
                assertTrue("angulo relativo " + relativo, relativo <= limite);
                if (c.acelerador() < 0) {
                    maximoReversa = Math.max(maximoReversa, relativo);
                }
                assertLanzaPegada(auto, chata);
            }
        }
        // En reversa la chata se acuchilla hasta el tope
        assertEquals(Chata.ANGULO_MAXIMO, maximoReversa, 0.01f);
    }

    /** El mismo recorrido a 30 y a 144 FPS deja la chata en el mismo lugar y angulo. */
    public void testChataIndependienteDeLosFps() {
        float[] lento = recorridoChata(1 / 30f);
        float[] rapido = recorridoChata(1 / 144f);
        assertEquals(lento[0], rapido[0], 0.6f);
        assertEquals(lento[1], rapido[1], 0.6f);
        assertEquals(lento[2], rapido[2], 0.05f);
    }

    private float[] recorridoChata(float dt) {
        Auto auto = new Auto(0, 0, 0);
        Chata chata = new Chata(auto);
        conducir(auto, chata, dt, 1.5f, new ControlesAuto(1, 0, false), SIN_OBSTACULOS, SIN_OBSTACULOS);
        conducir(auto, chata, dt, 1.5f, new ControlesAuto(1, 1, false), SIN_OBSTACULOS, SIN_OBSTACULOS);
        conducir(auto, chata, dt, 1.0f, new ControlesAuto(0, -1, true), SIN_OBSTACULOS, SIN_OBSTACULOS);
        conducir(auto, chata, dt, 1.0f, new ControlesAuto(-1, 1, false), SIN_OBSTACULOS, SIN_OBSTACULOS);
        return new float[] { chata.ejeX, chata.ejeZ, chata.angulo };
    }

    /** Maniobrar contra manzanas nunca deja la chata (ni el auto) dentro de una manzana. */
    public void testChataNoAtraviesaManzanas() {
        Obstaculos paraAuto = (xa, za, x, z) -> mapa.puedeCircular(x, z, Auto.RADIO);
        Obstaculos paraChata = (xa, za, x, z) -> mapa.puedeCircular(x, z, Chata.RADIO);
        // Calle vertical en x = -30, mirando al este hacia una manzana; parque al oeste
        Auto auto = new Auto(-30, -20, (float) (-Math.PI / 2));
        Chata chata = new Chata(auto);
        assertTrue(Chata.cabe(chata.estado(), chata.estado(), paraChata));
        ControlesAuto[] maniobras = {
                new ControlesAuto(1, 1, false), new ControlesAuto(-1, -1, false),
                new ControlesAuto(1, -1, false), new ControlesAuto(-1, 1, false),
                new ControlesAuto(-1, 0, false), new ControlesAuto(1, 0, false)
        };
        float dt = 1 / 60f;
        for (ControlesAuto c : maniobras) {
            for (int i = 0; i < 60 * 4; i++) {
                chata.conducir(dt, auto, c, paraAuto, paraChata);
                assertTrue(mapa.puedeCircular(auto.x, auto.z, Auto.RADIO));
                float[] circulos = chata.estado().circulos();
                assertTrue("chata dentro de una manzana en " + circulos[0] + "," + circulos[1],
                        mapa.puedeCircular(circulos[0], circulos[1], Chata.RADIO));
                assertTrue("chata dentro de una manzana en " + circulos[2] + "," + circulos[3],
                        mapa.puedeCircular(circulos[2], circulos[3], Chata.RADIO));
                assertLanzaPegada(auto, chata);
            }
        }
    }

    /** R: el auto vuelve a la salida y la chata queda recta detras de el. */
    public void testReinicioAlineaChata() {
        float[] salida = mapa.salida();
        Auto auto = new Auto(salida[0], salida[1], salida[2]);
        Chata chata = new Chata(auto);
        conducir(auto, chata, 1 / 60f, 2, new ControlesAuto(1, 1, false), SIN_OBSTACULOS, SIN_OBSTACULOS);
        auto.reiniciar();
        chata.alinear(auto);
        assertEquals(salida[2], chata.angulo, 1e-6f);
        float distancia = Chata.ENGANCHE + Chata.LANZA;
        assertEquals(salida[0] - auto.frenteX() * distancia, chata.ejeX, 1e-4f);
        assertEquals(salida[1] - auto.frenteZ() * distancia, chata.ejeZ, 1e-4f);
        float[] circulos = chata.estado().circulos();
        assertTrue(mapa.puedeCircular(circulos[0], circulos[1], Chata.RADIO));
        assertTrue(mapa.puedeCircular(circulos[2], circulos[3], Chata.RADIO));
    }
}
