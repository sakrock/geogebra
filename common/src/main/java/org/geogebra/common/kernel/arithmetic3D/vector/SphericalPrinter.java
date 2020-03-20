package org.geogebra.common.kernel.arithmetic3D.vector;

import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.printing.printable.vector.PrintableVector;
import org.geogebra.common.kernel.printing.printer.Printer;
import org.geogebra.common.kernel.printing.printer.expression.ExpressionPrinter;

class SphericalPrinter implements Printer {

    private PrintableVector vector;

    SphericalPrinter(PrintableVector vector) {
        this.vector = vector;
    }

    @Override
    public String print(StringTemplate tpl, ExpressionPrinter expressionPrinter) {
        return "point(("
                + expressionPrinter.print(vector.getX(), tpl)
                + ")*cos("
                + expressionPrinter.print(vector.getY(), tpl)
                + ")*cos("
                + expressionPrinter.print(vector.getZ(), tpl)
                + "),("
                + expressionPrinter.print(vector.getX(), tpl)
                + ")*sin("
                + expressionPrinter.print(vector.getY(), tpl)
                + ")*cos("
                + expressionPrinter.print(vector.getZ(), tpl)
                + "),("
                + expressionPrinter.print(vector.getX(), tpl)
                + ")*sin("
                + expressionPrinter.print(vector.getZ(), tpl)
                + "))";
    }
}
