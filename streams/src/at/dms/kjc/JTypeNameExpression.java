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
 * $Id: JTypeNameExpression.java,v 1.12 2006-12-20 18:03:33 dimock Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * A 'int.class' expression
 */
public class JTypeNameExpression extends JExpression {

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
	 * 
	 */
	private static final long serialVersionUID = -1773591851337291426L;

	private JTypeNameExpression() {} // for cloner only

    /**
     * Construct a node in the parsing tree
     * @param where the line of this node in the source code
     */
    public JTypeNameExpression(TokenReference where, String qualifiedName) {
        super(where);

        type = CClassType.lookup(qualifiedName);
    }

    /**
     * Construct a node in the parsing tree
     * @param where the line of this node in the source code
     */
    public JTypeNameExpression(TokenReference where, CClassType type) {
        super(where);

        this.type = type;
    }

    // ----------------------------------------------------------------------
    // ACCESSORS
    // ----------------------------------------------------------------------

    /**
     * Compute the type of this expression (called after parsing)
     * @return the type of this expression
     */
    @Override
	public CType getType() {
        return type;
    }
    /**
     * must be a CClassType
     */
    @Override
	public void setType(CType type) {
        assert type instanceof CClassType;
        this.type = (CClassType)type;
    }

    /**
     * Compute the type of this expression (called after parsing)
     * @return the type of this expression
     */
    public CClassType getClassType() {
        return type;
    }

    /**
     * Returns a qualified name for the type of this
     */
    public String getQualifiedName() {
        return type.getQualifiedName();
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
        try {
            type.checkType(context);
        } catch (UnpositionedError e) {
            throw e.addPosition(getTokenReference());
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
        p.visitTypeNameExpression(this, type);
    }

    /**
     * Accepts the specified attribute visitor
     * @param   p       the visitor
     */
    @Override
	public Object accept(AttributeVisitor p) {
        return    p.visitTypeNameExpression(this, type);
    }

    /**
     * Accepts the specified visitor
     * @param p the visitor
     * @param o object containing extra data to be passed to visitor
     * @return object containing data generated by visitor 
     */
    @Override
    public <S,T> S accept(ExpressionVisitor<S,T> p, T o) {
        return p.visitTypeName(this,o);
    }


    /**
     * Generates JVM bytecode to evaluate this expression.
     *
     * @param   code        the bytecode sequence
     * @param   discardValue    discard the result of the evaluation ?
     */
    @Override
	public void genCode(CodeSequence code, boolean discardValue) {
        if (! discardValue) {
            setLineNumber(code);
            // do nothing here
        }
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    private CClassType      type;

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    @Override
	public Object deepClone() {
        at.dms.kjc.JTypeNameExpression other = new at.dms.kjc.JTypeNameExpression();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.JTypeNameExpression other) {
        super.deepCloneInto(other);
        other.type = (at.dms.kjc.CClassType)at.dms.kjc.AutoCloner.cloneToplevel(this.type);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
