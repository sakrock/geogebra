/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package org.geogebra.common.kernel.algos;

import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Matrix.Coords;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoNumberValue;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoPolygon;
import org.geogebra.common.kernel.kernelND.GeoDirectionND;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.kernel.prover.NoSymbolicParametersException;
import org.geogebra.common.kernel.prover.polynomial.PPolynomial;
import org.geogebra.common.kernel.prover.polynomial.PVariable;

/**
 * Creates a regular Polygon for two points and the number of vertices.
 * 
 * @author Markus Hohenwarter
 */
public class AlgoPolygonRegular extends AlgoPolygonRegularND
		implements SymbolicParametersBotanaAlgo {

	private Coords centerPointCoords;

	private PPolynomial[] botanaPolynomials;
	private PVariable[] botanaVars;

	/**
	 * Creates a new regular polygon algorithm
	 * 
	 * @param c
	 *            construction
	 * @param labels
	 *            labels[0] for polygon, then labels for segments and then for
	 *            points
	 * @param A1
	 *            first input point
	 * @param B1
	 *            second input point
	 * @param num
	 *            number of vertices
	 */
	public AlgoPolygonRegular(Construction c, String[] labels, GeoPointND A1,
			GeoPointND B1, GeoNumberValue num) {
		super(c, labels, A1, B1, num, null);
	}

	@Override
	protected GeoPolygon newGeoPolygon(Construction cons1) {
		return new GeoPolygon(cons1);
	}

	@Override
	protected GeoElement newGeoPoint(Construction cons1) {
		GeoPoint newPoint = new GeoPoint(cons1);
		newPoint.setCoords(0, 0, 1);
		return newPoint;
	}

	@Override
	protected void setCenterPoint(int n, double beta) {

		double xA = ((GeoPoint) A).inhomX;
		double yA = ((GeoPoint) A).inhomY;

		double xB = ((GeoPoint) B).inhomX;
		double yB = ((GeoPoint) B).inhomY;

		// some temp values
		double mx = (xA + xB) / 2; // midpoint of AB
		double my = (yA + yB) / 2;
		// normal vector of AB
		double nx = yA - yB;
		double ny = xB - xA;

		// center point of regular polygon
		double tanBetaHalf = Math.tan(beta) / 2;
		centerPoint.setCoords(mx + tanBetaHalf * nx, my + tanBetaHalf * ny,
				1.0);
		centerPointCoords = centerPoint.getInhomCoords();
	}

	@Override
	protected void rotate(GeoPointND point) {
		point.rotate(rotAngle, centerPointCoords);
	}

	// for AlgoElement
	@Override
	protected void setInputOutput() {
		input = new GeoElement[3];
		input[0] = (GeoElement) A;
		input[1] = (GeoElement) B;
		input[2] = num.toGeoElement();
		// set dependencies
		for (int i = 0; i < input.length; i++) {
			input[i].addAlgorithm(this);
		}
		cons.addToAlgorithmList(this);

		// setOutput(); done in compute

		// parent of output
		getPoly().setParentAlgorithm(this);

	}

	@Override
	final protected void setDirection(GeoDirectionND direction) {
		// used only in 3D
	}

	@Override
	public void calcCentroid(GeoPoint p) {
		p.setCoords((GeoPoint) centerPoint);
	}

	@Override
	public PVariable[] getBotanaVars(GeoElementND geo) {
		/*
		 * The Polygon(A,B,n) command creates n-2 new points. We distinguish
		 * between them by checking which geo was created by the command.
		 */
		PVariable vars[] = new PVariable[2];
		for (int i = 0; i < outputPoints.size(); ++i) {
			if (geo.equals(outputPoints.getElement(i))) {
				vars[0] = botanaVars[2 * i];
				vars[1] = botanaVars[2 * i + 1];
				return vars;
			}
		}
		return null; // Here maybe an exception should be thrown...?
	}

	@Override
	public PPolynomial[] getBotanaPolynomials(GeoElementND geo)
			throws NoSymbolicParametersException {
		if (botanaPolynomials != null) {
			return botanaPolynomials;
		}

		PVariable[] varsA = new PVariable[2];
		PVariable[] varsB = new PVariable[2];
		varsA = ((SymbolicParametersBotanaAlgo) A).getBotanaVars(A);
		varsB = ((SymbolicParametersBotanaAlgo) B).getBotanaVars(B);
		
		int sides = (int) num.getDouble();

		if (sides == 4) {
			PVariable[] varsC = new PVariable[2];
			varsC[0] = new PVariable(kernel);
			varsC[1] = new PVariable(kernel);
			PVariable[] varsD = new PVariable[2];
			varsD[0] = new PVariable(kernel);
			varsD[1] = new PVariable(kernel);
			botanaPolynomials = new PPolynomial[4];
			/* b1-a1 = c2-b2, a2-b2=c1-b1, d1-a1=a2-b2, d2-a2=b1-a1 */
			PPolynomial a1 = new PPolynomial(varsA[0]);
			PPolynomial b1 = new PPolynomial(varsB[0]);
			PPolynomial c1 = new PPolynomial(varsC[0]);
			PPolynomial d1 = new PPolynomial(varsD[0]);
			PPolynomial a2 = new PPolynomial(varsA[1]);
			PPolynomial b2 = new PPolynomial(varsB[1]);
			PPolynomial c2 = new PPolynomial(varsC[1]);
			PPolynomial d2 = new PPolynomial(varsD[1]);
			botanaPolynomials[0] = b1.subtract(a1).subtract(c2).add(b2);
			botanaPolynomials[1] = a2.subtract(b2).subtract(c1).add(b1);
			botanaPolynomials[2] = d1.subtract(a1).subtract(a2).add(b2);
			botanaPolynomials[3] = d2.subtract(a2).subtract(b1).add(a1);
			botanaVars = new PVariable[(sides - 2) * 2];
			botanaVars[0] = varsC[0];
			botanaVars[1] = varsC[1];
			botanaVars[2] = varsD[0];
			botanaVars[3] = varsD[1];
			return botanaPolynomials;
		}

		// The other cases are not yet implemented.
		throw new NoSymbolicParametersException();

	}

}
