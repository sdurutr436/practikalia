package practikalia.asignacion;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * El curso académico, identificado por su año de inicio: el curso {@code 2026}
 * es «2026/2027» y va del 1 de septiembre de 2026 al 1 de julio de 2027. Es el
 * identificador con el que la pantalla de asignaciones decide qué alumnado
 * tiene delante: se asume que las prácticas se hacen en el curso de matrícula.
 *
 * ponytail: julio y agosto siguen contando como el curso que acaba de terminar.
 * En ese hueco no hay ningún curso vigente, y dejar la pantalla vacía dos meses
 * es peor que seguir enseñando el último; si algún día hay que distinguirlos,
 * el cambio es {@link #de(LocalDate)} y nada más.
 */
public final class Curso {

    private Curso() {
    }

    /** El año de inicio del curso vigente hoy. */
    public static int actual() {
        return de(LocalDate.now());
    }

    static int de(LocalDate dia) {
        return dia.getMonthValue() >= 9 ? dia.getYear() : dia.getYear() - 1;
    }

    /**
     * El instante en que arranca un curso. Sirve para situar en su curso al
     * alumnado sin año de matrícula, que es opcional en el alta y en el CSV:
     * en ese caso vale el curso en el que se le dio de alta.
     */
    public static Instant inicio(int curso) {
        return LocalDate.of(curso, 9, 1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}
