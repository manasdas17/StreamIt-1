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
 * $Id: AlternativeBlock.java,v 1.2 2006-01-25 17:00:49 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import at.dms.compiler.tools.antlr.runtime.Token;
import at.dms.compiler.tools.antlr.runtime.Vector;

/**
 * A list of alternatives
 */
class AlternativeBlock extends AlternativeElement {
    protected String initAction = null; // string for init action {...}
    protected Vector alternatives;  // Contains Alternatives

    protected String label;         // can label a looping block to break out of it.

    protected int alti, altj;       // which alts are being compared at the moment with
    // deterministic()?
    protected int analysisAlt;      // which alt are we computing look on?  Must be alti or altj

    protected boolean hasAnAction = false;  // does any alt have an action?
    protected boolean hasASynPred = false;  // does any alt have a syntactic predicate?

    protected int ID=0;             // used to generate unique variables
    protected static int nblks; // how many blocks have we allocated?
    boolean not = false;                // true if block is inverted.

    boolean greedy = true;          // Blocks are greedy by default
    boolean greedySet=false;        // but, if not explicitly greedy, warning might be generated

    protected boolean doAutoGen=true;   // false if no text to be generated for block

    protected boolean warnWhenFollowAmbig = true; // warn when an empty path or exit path

    protected boolean generateAmbigWarnings = true;  // the general warning "shut-up" mechanism

    // conflicts with alt of subrule.
    // Turning this off will suppress stuff
    // like the if-then-else ambig.

    public AlternativeBlock(Grammar g) {
        this(g,0,false);
    }

    public AlternativeBlock(Grammar g, int line, boolean not) {
        super(g);
        alternatives = new Vector(5);
        this.line = line;
        this.not = not;
        nblks++;
        ID = nblks;
    }

    public void addAlternative(Alternative alt) {
        alternatives.appendElement(alt);
    }
    public void generate(JavaCodeGenerator generator) {
        generator.gen(this);
    }
    public Alternative getAlternativeAt(int i) {
        return (Alternative)alternatives.elementAt(i);
    }
    public Vector getAlternatives() {
        return alternatives;
    }
    public boolean getAutoGen() {
        return doAutoGen;
    }
    public String getInitAction() {
        return initAction;
    }
    public String getLabel() {
        return label;
    }
    public Lookahead look(int k) {
        return grammar.theLLkAnalyzer.look(k, this);
    }
    public void prepareForAnalysis() {
        for (int i=0; i<alternatives.size(); i++) {
            // deterministic() uses an alternative cache and sets lookahead depth
            Alternative a = (Alternative)alternatives.elementAt(i);
            a.cache = new Lookahead[grammar.maxk+1];
            a.lookaheadDepth = GrammarAnalyzer.LOOKAHEAD_DEPTH_INIT;
        }
    }
    /**
     * Walk the syntactic predicate and, for a rule ref R, remove
     * the ref from the list of FOLLOW references for R (stored
     * in the symbol table.
     */
    public void removeTrackingOfRuleRefs(Grammar g) {
        for (int i=0; i<alternatives.size(); i++) {
            Alternative alt = getAlternativeAt(i);
            AlternativeElement elem = alt.head;
            while ( elem!=null ) {
                if ( elem instanceof RuleRefElement ) {
                    RuleRefElement rr = (RuleRefElement)elem;
                    RuleSymbol rs = (RuleSymbol)g.getSymbol(rr.targetRule);
                    if ( rs==null ) {
                        grammar.tool.error("rule "+rr.targetRule+" referenced in (...)=>, but not defined");
                    } else {
                        rs.references.removeElement(rr);
                    }
                } else if ( elem instanceof AlternativeBlock ) {// recurse into subrules
                    ((AlternativeBlock)elem).removeTrackingOfRuleRefs(g);
                }
                elem = elem.next;
            }
        }
    }
    public void setAlternatives(Vector v) {
        alternatives = v;
    }
    public void setAutoGen(boolean doAutoGen_) {
        doAutoGen = doAutoGen_;
    }
    public void setInitAction(String initAction_) {
        initAction = initAction_;
    }
    public void setLabel(String label_) {
        label = label_;
    }

    public void setOption(Token key, Token value) {
        if (key.getText().equals("warnWhenFollowAmbig")) {
            if (value.getText().equals("true")) {
                warnWhenFollowAmbig = true;
            } else if (value.getText().equals("false")) {
                warnWhenFollowAmbig = false;
            } else {
                grammar.tool.error("Value for warnWhenFollowAmbig must be true or false", grammar.getFilename(), key.getLine());
            }
        } else if (key.getText().equals("generateAmbigWarnings")) {
            if (value.getText().equals("true")) {
                generateAmbigWarnings = true;
            } else if (value.getText().equals("false")) {
                generateAmbigWarnings = false;
            } else {
                grammar.tool.error("Value for generateAmbigWarnings must be true or false", grammar.getFilename(), key.getLine());
            }
        } else if (key.getText().equals("greedy")) {
            if (value.getText().equals("true")) {
                greedy = true;
                greedySet = true;
            } else if (value.getText().equals("false")) {
                greedy = false;
                greedySet = true;
            } else {
                grammar.tool.error("Value for greedy must be true or false", grammar.getFilename(), key.getLine());
            }
        } else {
            grammar.tool.error("Invalid subrule option: " + key.getText(), grammar.getFilename(), key.getLine());
        }
    }

    public String toString() {
        String s=" (";
        if ( initAction!=null ) {
            s += initAction;
        }
        for (int i=0; i<alternatives.size(); i++) {
            Alternative alt = getAlternativeAt(i);
            Lookahead[] cache = alt.cache;
            int k = alt.lookaheadDepth;
            // dump lookahead set
            if ( k==GrammarAnalyzer.LOOKAHEAD_DEPTH_INIT ) {
            } else if ( k==GrammarAnalyzer.NONDETERMINISTIC ) {
                s += "{?}:";
            } else {
                s += " {";
                for (int j=1; j<=k; j++) {
                    s += cache[j].toString(",",grammar.tokenManager.getVocabulary());
                    if ( j<k && cache[j+1]!=null ) {
                        s += ";";
                    }
                }
                s += "}:";
            }
            // dump alternative including pred (if any)
            AlternativeElement p = alt.head;
            String pred = alt.semPred;
            if ( pred!=null ) {
                s += pred;
            }
            while (p!=null ) {
                s += p;
                p = p.next;
            }
            if ( i<(alternatives.size()-1) ) {
                s += " |";
            }
        }
        s += " )";
        return s;
    }
}
