/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: JUnaryPlusExpression.java,v 1.8 2006-10-27 20:48:55 dimock Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * JLS 15.15.3 Unary Plus Operator +
 */
public class JUnaryPlusExpression extends JUnaryExpression {

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    protected JUnaryPlusExpression() {} // for cloner only

    /**
     * Construct a node in the parsing tree
     * @param   where       the line of this node in the source code
     * @param   expr        the operand
     */
    public JUnaryPlusExpression(TokenReference where, JExpression expr) {
        super(where, expr);
    }

    // ----------------------------------------------------------------------
    // ACCESSORS
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // SEMANTIC ANALYSIS
    // ----------------------------------------------------------------------

    /**
     * Analyses the expression (semantically).
     * @param   context     the analysis context
     * @return  an equivalent, analysed expression
     * @exception   PositionedError the analysis detected an error
     */
    public JExpression analyse(CExpressionContext context) throws PositionedError {
        expr = expr.analyse(context);
        check(context, expr.getType().isNumeric(), KjcMessages.UNARY_BADTYPE_PM, expr.getType());
        type = CNumericType.unaryPromote(expr.getType());
        expr = expr.convertType(type, context);

        return expr;
    }

    // ----------------------------------------------------------------------
    // CODE GENERATION
    // ----------------------------------------------------------------------

    /**
     * Accepts the specified visitor
     * @param   p       the visitor
     */
    public void accept(KjcVisitor p) {
        p.visitUnaryPlusExpression(this, expr);
    }

    /**
     * Accepts the specified attribute visitor
     * @param   p       the visitor
     */
    public Object accept(AttributeVisitor<?> p) {
        return    p.visitUnaryPlusExpression(this, expr);
    }

    /**
     * Accepts the specified visitor
     * @param p the visitor
     * @param o object containing extra data to be passed to visitor
     * @return object containing data generated by visitor 
     */
    @Override
    public <S,T> S accept(ExpressionVisitor<S,T> p, T o) {
        return p.visitUnaryPlus(this,o);
    }


    /**
     * Generates JVM bytecode to evaluate this expression.
     *
     * @param   code        the bytecode sequence
     * @param   discardValue    discard the result of the evaluation ?
     */
    public void genCode(CodeSequence code, boolean discardValue) {
        setLineNumber(code);
        expr.genCode(code, discardValue);
    }

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() {
        at.dms.kjc.JUnaryPlusExpression other = new at.dms.kjc.JUnaryPlusExpression();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.JUnaryPlusExpression other) {
        super.deepCloneInto(other);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
