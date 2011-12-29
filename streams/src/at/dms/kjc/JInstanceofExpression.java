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
 * $Id: JInstanceofExpression.java,v 1.12 2006-12-20 18:03:33 dimock Exp $
 */

package at.dms.kjc;

import at.dms.compiler.CWarning;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * This class represents a instanceof expression.
 */
public class JInstanceofExpression extends JExpression {

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
	 * 
	 */
	private static final long serialVersionUID = -7896550168381459414L;

	protected JInstanceofExpression() {} // for cloner only

    /**
     * Construct a node in the parsing tree
     * This method is directly called by the parser
     *
     * @param   where   the line of this node in the source code
     * @param   expr    the expression to be casted
     * @param   dest    the type to test for
     */
    public JInstanceofExpression(TokenReference where, JExpression expr, CType dest) {
        super(where);
        this.expr = expr;
        this.dest = dest;
    }

    // ----------------------------------------------------------------------
    // ACCESSORS
    // ----------------------------------------------------------------------

    /**
     * Compute the type of this expression.
     *
     * @return the type of this expression
     */
    @Override
	public CType getType() {
        return CStdType.Boolean;
    }
    
    /**
     * Manifest type CStdType.Boolean; do not set to anything else.
     */
    
    @Override
	public void setType(CType type) {
        assert type == getType();
    }

    // ----------------------------------------------------------------------
    // SEMANTIC ANALYSIS
    // ----------------------------------------------------------------------

    /**
     * Analyses the expression (semantically).
     * @param   context     the analysis context
     * @return  an equivalent, analysed expression
     * @exception   PositionedError the analysis detected an error
     */
    @Override
	public JExpression analyse(CExpressionContext context) throws PositionedError {
        expr = expr.analyse(context);
        try {
            dest.checkType(context);
        } catch (UnpositionedError e) {
            throw e.addPosition(getTokenReference());
        }
        check(context, expr.getType().isReference(),
              KjcMessages.INSTANCEOF_BADTYPE, expr.getType(), dest);

        check(context, dest.isCastableTo(expr.getType()),
              KjcMessages.INSTANCEOF_BADTYPE, expr.getType(), dest);

        if (expr.getType().isAssignableTo(dest)) {
            context.reportTrouble(new CWarning(getTokenReference(), KjcMessages.UNNECESSARY_INSTANCEOF, null));
        }

        return this;
    }

    // ----------------------------------------------------------------------
    // CODE GENERATION
    // ----------------------------------------------------------------------

    /**
     * Accepts the specified visitor
     * @param   p       the visitor
     */
    @Override
	public void accept(KjcVisitor p) {
        p.visitInstanceofExpression(this, expr, dest);
    }

    /**
     * Accepts the specified attribute visitor
     * @param   p       the visitor
     */
    @Override
	public Object accept(AttributeVisitor p) {
        return    p.visitInstanceofExpression(this, expr, dest);
    }

    /**
     * Accepts the specified visitor
     * @param p the visitor
     * @param o object containing extra data to be passed to visitor
     * @return object containing data generated by visitor 
     */
    @Override
    public <S,T> S accept(ExpressionVisitor<S,T> p, T o) {
        return p.visitInstanceof(this,o);
    }


    /**
     * Generates JVM bytecode to evaluate this expression.
     *
     * @param   code        the bytecode sequence
     * @param   discardValue    discard the result of the evaluation ?
     */
    @Override
	public void genCode(CodeSequence code, boolean discardValue) {
        setLineNumber(code);

        expr.genCode(code, false);
        code.plantClassRefInstruction(opc_instanceof, ((CClassType)dest).getQualifiedName());

        if (discardValue) {
            code.plantPopInstruction(getType());
        }
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    private JExpression     expr;
    private CType           dest;

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    @Override
	public Object deepClone() {
        at.dms.kjc.JInstanceofExpression other = new at.dms.kjc.JInstanceofExpression();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.JInstanceofExpression other) {
        super.deepCloneInto(other);
        other.expr = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.expr);
        other.dest = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.dest);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
