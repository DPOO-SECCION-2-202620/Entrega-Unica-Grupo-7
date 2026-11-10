package modelo.ventas;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinero {

    private Dinero() {
    }

    public static double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double porcentaje(double valor, String tasa) {
        return BigDecimal.valueOf(valor).multiply(new BigDecimal(tasa)).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double subtotal(java.util.List<DetalleVenta> detalles) {
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleVenta detalle : detalles) {
            total = total.add(BigDecimal.valueOf(detalle.getPrecioUnitario())
                    .multiply(BigDecimal.valueOf(detalle.getCantidad())));
        }
        return total.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static long puntos(double pagado) {
        return BigDecimal.valueOf(pagado).multiply(new BigDecimal("0.02")).setScale(0, RoundingMode.FLOOR)
                .longValueExact();
    }
}
