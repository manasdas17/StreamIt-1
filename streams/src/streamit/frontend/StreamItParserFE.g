/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

/*
 * StreamItParserFE.g: StreamIt parser producing front-end tree
 * $Id: StreamItParserFE.g,v 1.44 2003-10-14 19:10:40 dmaze Exp $
 */

header {
	package streamit.frontend;

	import streamit.frontend.nodes.*;

	import java.util.Collections;
	import java.io.DataInputStream;
	import java.util.List;

	import java.util.ArrayList;
}

class StreamItParserFE extends Parser;
options {
	importVocab=StreamItLex;	// use vocab generated by lexer
}

{
	public static void main(String[] args)
	{
		try
		{
			DataInputStream dis = new DataInputStream(System.in);
			StreamItLex lexer = new StreamItLex(dis);
			StreamItParserFE parser = new StreamItParserFE(lexer);
			parser.program();
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
		}
	}

	public FEContext getContext(Token t)
	{
		int line = t.getLine();
		if (line == 0) line = -1;
		int col = t.getColumn();
		if (col == 0) col = -1;
		return new FEContext(getFilename(), line, col);
	}

	private boolean hasError = false;

	public void reportError(RecognitionException ex)
	{
		hasError = true;
		super.reportError(ex);
	}

	public void  reportError(String s)
	{
		hasError = true;
		super.reportError(s);
	}
}

program	returns [Program p]
{ p = null; List structs = new ArrayList(); List streams = new ArrayList();
	TypeStruct ts; StreamSpec ss; }
	:	( ts=struct_decl { structs.add(ts); }
		| ss=stream_decl { streams.add(ss); }
		)*
		EOF
		// Can get away with no context here.
		{ if (!hasError) p = new Program(null, streams, structs); }
	;

stream_decl returns [StreamSpec ss] { ss = null; StreamType st; }
	:	st=stream_type_decl
		(ss=filter_decl[st] | ss=struct_stream_decl[st])
	;

filter_decl[StreamType st] returns [StreamSpec ss]
{ ss = null; List params = Collections.EMPTY_LIST; FEContext context = null; }
	:	tf:TK_filter
		{ if (st != null) context = st.getContext();
			else context = getContext(tf); }
		id:ID
		(params=param_decl_list)?
		ss=filter_body[context, st, id.getText(), params]
	;

filter_body[FEContext context, StreamType st, String name, List params]
returns [StreamSpec ss]
{ ss = null; List vars = new ArrayList(); List funcs = new ArrayList();
	Function fn; FieldDecl decl; }
	:	LCURLY
		( fn=init_decl { funcs.add(fn); }
		| fn=work_decl { funcs.add(fn); }
		| (data_type ID LPAREN) => fn=function_decl { funcs.add(fn); }
		| fn=handler_decl { funcs.add(fn); }
		| decl=field_decl SEMI { vars.add(decl); }
		)*
		RCURLY
		{ ss = new StreamSpec(context, StreamSpec.STREAM_FILTER,
				st, name, params, vars, funcs); }
	;

field_decl returns [FieldDecl f] { f = null; Type t; Expression x = null;
	List ts = new ArrayList(); List ns = new ArrayList();
	List xs = new ArrayList(); FEContext ctx = null; }
	:	t=data_type id:ID (ASSIGN x=right_expr)?
		{ ctx = getContext(id); ts.add(t); ns.add(id.getText()); xs.add(x); }
		(
			{ x = null; }
			COMMA id2:ID (ASSIGN x=right_expr)?
			{ ts.add(t); ns.add(id2.getText()); xs.add(x); }
		)*
		{ f = new FieldDecl(ctx, ts, ns, xs); }
	;


stream_type_decl returns [StreamType st] { st = null; Type in, out; }
	:	in=data_type t:ARROW out=data_type
		// Again, want context from the input type, but Types aren't
		// FENodes.
		{ st = new StreamType(getContext(t), in, out); }
	;

struct_stream_decl[StreamType st] returns [StreamSpec ss]
{ ss = null; int type = 0;
	List params = Collections.EMPTY_LIST; Statement body; }
	:	( TK_pipeline { type = StreamSpec.STREAM_PIPELINE; }
		| TK_splitjoin { type = StreamSpec.STREAM_SPLITJOIN; }
		| TK_feedbackloop { type = StreamSpec.STREAM_FEEDBACKLOOP; }
		)
		id:ID
		(params=param_decl_list)?
		body=block
		{ ss = new StreamSpec(st.getContext(), type, st, id.getText(),
				params, body); }
	;

work_decl returns [FuncWork f]
{	f = null;
	Expression pop = null, peek = null, push = null;
	Statement s; FEContext c = null; String name = null;
	int type = 0;
}
	:	(	tw:TK_work { c = getContext(tw); type = Function.FUNC_WORK; }
		|	tpw:TK_prework { c = getContext(tpw);
							 type = Function.FUNC_PREWORK; }
		|	tp:TK_phase id:ID { c = getContext(tp); name = id.getText();
			                    type = Function.FUNC_PHASE;}
		)
		(	TK_push push=right_expr
		|	TK_pop pop=right_expr
		|	TK_peek peek=right_expr
		)*
		s=block
		{ f = new FuncWork(c, type, name, s, peek, pop, push); }
	;

init_decl returns [Function f] { Statement s; f = null; }
	:	t:TK_init s=block { f = Function.newInit(getContext(t), s); }
	;

push_statement returns [Statement s] { s = null; Expression x; }
	:	t:TK_push LPAREN x=right_expr RPAREN
		{ s = new StmtPush(getContext(t), x); }
	;

msg_statement returns [Statement s] { s = null; List l;
  Expression minl = null, maxl = null; }
	:	p:ID DOT m:ID l=func_call_params
		(LSQUARE (minl=right_expr)? COLON (maxl=right_expr)? RSQUARE)?
		{ s = new StmtSendMessage(getContext(p),
				new ExprVar(getContext(p), p.getText()),
				m.getText(), l, minl, maxl); }
	;

statement returns [Statement s] { s = null; }
	:	s=add_statement
	|	s=body_statement
	| 	s=loop_statement
	|	s=split_statement SEMI
	|	s=join_statement SEMI
	|	s=enqueue_statement SEMI
	|	s=push_statement SEMI
	|	s=block
	|	(data_type ID) => s=variable_decl SEMI!
	|	(expr_statement) => s=expr_statement SEMI!
	|	tb:TK_break SEMI { s = new StmtBreak(getContext(tb)); }
	|	tc:TK_continue SEMI { s = new StmtContinue(getContext(tc)); }
	|	s=return_statement SEMI
	|	s=if_else_statement
	|	s=while_statement
	|	s=do_while_statement SEMI
	|	s=for_statement
	|	s=msg_statement SEMI
	;

add_statement returns [Statement s] { s = null; StreamCreator sc; }
	: t:TK_add sc=stream_creator { s = new StmtAdd(getContext(t), sc); }
	;

body_statement returns [Statement s] { s = null; StreamCreator sc; }
	: t:TK_body sc=stream_creator { s = new StmtBody(getContext(t), sc); }
	;

loop_statement returns [Statement s] { s = null; StreamCreator sc; }
	: t:TK_loop sc=stream_creator { s = new StmtLoop(getContext(t), sc); }
	;

stream_creator returns [StreamCreator sc] { sc = null; }
	: (ID ARROW | ~ID) => sc=anonymous_stream
	| sc=named_stream SEMI
	;

portal_spec returns [List p] { p = null; Expression pn; }
	:	TK_to id:ID
			{ pn = new ExprVar(getContext(id), id.getText());
			  p = Collections.singletonList(pn); }
	;

anonymous_stream returns [StreamCreator sc]
{ sc = null; StreamType st = null; List params = new ArrayList();
Statement body; List types = new ArrayList(); Type t; StreamSpec ss = null;
List p = null; int sst = 0; FEContext ctx = null; }
	: (st=stream_type_decl)?
		( tf:TK_filter
			ss=filter_body[getContext(tf), st, null, Collections.EMPTY_LIST]
			((p=portal_spec)? SEMI)?
			{ sc = new SCAnon(getContext(tf), ss, p); }
		|	( tp:TK_pipeline
				{ ctx = getContext(tp); sst = StreamSpec.STREAM_PIPELINE; }
			| ts:TK_splitjoin
				{ ctx = getContext(ts); sst = StreamSpec.STREAM_SPLITJOIN; }
			| tl:TK_feedbackloop
				{ ctx = getContext(tl); sst = StreamSpec.STREAM_FEEDBACKLOOP; }
			) body=block ((p=portal_spec)? SEMI)?
			{ sc = new SCAnon(ctx, sst, body, p); }
		)
	;

named_stream returns [StreamCreator sc]
{ sc = null; List params = new ArrayList(); List types = new ArrayList();
Type t; List p = null; }
	: id:ID
		(LESS_THAN t=data_type MORE_THAN { types.add(t); })?
		(params=func_call_params)?
		(p=portal_spec)?
		{ sc = new SCSimple(getContext(id), id.getText(), types, params, p); }
	;

split_statement returns [Statement s] { s = null; SplitterJoiner sj; }
	: t:TK_split sj=splitter_or_joiner
		{ s = new StmtSplit(getContext(t), sj); }
	;

join_statement returns [Statement s] { s = null; SplitterJoiner sj; }
	: t:TK_join sj=splitter_or_joiner
		{ s = new StmtJoin(getContext(t), sj); }
	;

splitter_or_joiner returns [SplitterJoiner sj]
{ sj = null; Expression x; List l; }
	: tr:TK_roundrobin
		( (LPAREN RPAREN) => LPAREN RPAREN
			{ sj = new SJRoundRobin(getContext(tr)); }
		| (LPAREN right_expr RPAREN) => LPAREN x=right_expr RPAREN
			{ sj = new SJRoundRobin(getContext(tr), x); }
		| l=func_call_params { sj = new SJWeightedRR(getContext(tr), l); }
		| { sj = new SJRoundRobin(getContext(tr)); }
		)
	| td:TK_duplicate (LPAREN RPAREN)?
		{ sj = new SJDuplicate(getContext(td)); }
	;

enqueue_statement returns [Statement s] { s = null; Expression x; }
	: t:TK_enqueue x=right_expr { s = new StmtEnqueue(getContext(t), x); }
	;

data_type returns [Type t] { t = null; Expression x; }
	:	(t=primitive_type | id:ID { t = new TypeStructRef(id.getText()); })
		(	l:LSQUARE
			(x=right_expr { t = new TypeArray(t, x); }
			| { throw new SemanticException("missing array bounds in type declaration", getFilename(), l.getLine()); }
			)
			RSQUARE
		)*
	|	TK_void { t = new TypePrimitive(TypePrimitive.TYPE_VOID); }
	|	TK_portal LESS_THAN pn:ID MORE_THAN
		{ t = new TypePortal(pn.getText()); }
	;

primitive_type returns [Type t] { t = null; }
	:	TK_boolean { t = new TypePrimitive(TypePrimitive.TYPE_BOOLEAN); }
	|	TK_bit { t = new TypePrimitive(TypePrimitive.TYPE_BIT); }
	|	TK_int { t = new TypePrimitive(TypePrimitive.TYPE_INT); }
	|	TK_float { t = new TypePrimitive(TypePrimitive.TYPE_FLOAT); }
	|	TK_double { t =  new TypePrimitive(TypePrimitive.TYPE_DOUBLE); }
	|	TK_complex { t = new TypePrimitive(TypePrimitive.TYPE_COMPLEX); }
	;

variable_decl returns [Statement s] { s = null; Type t; Expression x = null; 
	List ts = new ArrayList(); List ns = new ArrayList();
	List xs = new ArrayList(); FEContext ctx = null; }
	:	t=data_type
		id:ID { ctx = getContext(id); }
		(ASSIGN x=right_expr)?
		{ ts.add(t); ns.add(id.getText()); xs.add(x); }
		(
			{ x = null; }
			COMMA id2:ID (ASSIGN x=right_expr)?
			{ ts.add(t); ns.add(id2.getText()); xs.add(x); }
		)*
		{ s = new StmtVarDecl(ctx, ts, ns, xs); }
		// NB: we'd use the context of t, except Type doesn't include
		// a context.  This is probably okay in the grand scheme of things.
		// We explicitly use the context of the first identifier.
	;

function_decl returns [Function f] { Type t; List l; Statement s; f = null;
int cls = Function.FUNC_HELPER; }
	:	t=data_type id:ID l=param_decl_list s=block
		{ f = new Function(getContext(id), cls, id.getText(), t, l, s); }
	;

handler_decl returns [Function f] { List l; Statement s; f = null;
Type t = new TypePrimitive(TypePrimitive.TYPE_VOID);
int cls = Function.FUNC_HANDLER; }
	:	TK_handler id:ID l=param_decl_list s=block
		{ f = new Function(getContext(id), cls, id.getText(), t, l, s); }
	;

param_decl_list returns [List l] { l = new ArrayList(); Parameter p; }
	:	LPAREN
		(p=param_decl { l.add(p); } (COMMA p=param_decl { l.add(p); })*
		)?
		RPAREN
	;

param_decl returns [Parameter p] { Type t; p = null; }
	:	t=data_type id:ID { p = new Parameter(t, id.getText()); }
	;

block returns [Statement s] { s = null; List l = new ArrayList(); }
	:	t:LCURLY ( s=statement { l.add(s); } )* RCURLY
		{ s = new StmtBlock(getContext(t), l); }
	;

return_statement returns [Statement s] { s = null; Expression x = null; }
	:	t:TK_return (x=right_expr)? { s = new StmtReturn(getContext(t), x); }
	;

if_else_statement returns [Statement s]
{ s = null; Expression x; Statement t, f = null; }
	:	u:TK_if LPAREN x=right_expr RPAREN t=statement
		((TK_else) => (TK_else f=statement))?
		{ s = new StmtIfThen(getContext(u), x, t, f); }
	;

while_statement returns [Statement s] { s = null; Expression x; Statement b; }
	:	t:TK_while LPAREN x=right_expr RPAREN b=statement
		{ s = new StmtWhile(getContext(t), x, b); }
	;

do_while_statement returns [Statement s]
{ s = null; Expression x; Statement b; }
	:	t:TK_do b=statement TK_while LPAREN x=right_expr RPAREN
		{ s = new StmtDoWhile(getContext(t), b, x); }
	;

for_statement returns [Statement s]
{ s = null; Expression x; Statement a, b, c; }
	:	t:TK_for LPAREN a=for_init_statement SEMI x=right_expr SEMI
		b=for_incr_statement RPAREN c=statement
		{ s = new StmtFor(getContext(t), a, x, b, c); }
	;

for_init_statement returns [Statement s] { s = null; }
	:	(variable_decl) => s=variable_decl
	|	(expr_statement) => s=expr_statement
	;

for_incr_statement returns [Statement s] { s = null; }
	:	s=expr_statement
	;

expr_statement returns [Statement s] { s = null; Expression x; }
	:	(incOrDec) => x=incOrDec { s = new StmtExpr(x); }
	|   (left_expr (ASSIGN | PLUS_EQUALS | MINUS_EQUALS | STAR_EQUALS |
				DIV_EQUALS)) => s=assign_expr
	|	(ID LPAREN) => x=func_call { s = new StmtExpr(x); }
	|	x=streamit_value_expr { s = new StmtExpr(x); }
	;

assign_expr returns [Statement s] { s = null; Expression l, r; int o = 0; }
	:	l=left_expr
		(	ASSIGN { o = 0; }
		|	PLUS_EQUALS { o = ExprBinary.BINOP_ADD; }
		| 	MINUS_EQUALS { o = ExprBinary.BINOP_SUB; }
		|	STAR_EQUALS { o = ExprBinary.BINOP_MUL; }
		|	DIV_EQUALS { o = ExprBinary.BINOP_DIV; }
		)
		r=right_expr
		{ s = new StmtAssign(l.getContext(), l, r, o); }
	;

func_call returns [Expression x] { x = null; List l; }
	:	name:ID l=func_call_params
		{ x = new ExprFunCall(getContext(name), name.getText(), l); }
	;

func_call_params returns [List l] { l = new ArrayList(); Expression x; }
	:	LPAREN
		(	x=right_expr { l.add(x); }
			(COMMA x=right_expr { l.add(x); })*
		)?
		RPAREN
	;

left_expr returns [Expression x] { x = null; }
	:	x=value
	;

right_expr returns [Expression x] { x = null; }
	:	x=ternaryExpr
	;

ternaryExpr returns [Expression x] { x = null; Expression b, c; }
	:	x=logicOrExpr
		(QUESTION b=ternaryExpr COLON c=ternaryExpr
			{ x = new ExprTernary(x.getContext(), ExprTernary.TEROP_COND,
					x, b, c); }
		)?
	;

logicOrExpr returns [Expression x] { x = null; Expression r; int o = 0; }
	:	x=logicAndExpr
		(LOGIC_OR r=logicAndExpr
			{ x = new ExprBinary(x.getContext(), ExprBinary.BINOP_OR, x, r); }
		)*
	;

logicAndExpr returns [Expression x] { x = null; Expression r; }
	:	x=bitwiseExpr
		(LOGIC_AND r=bitwiseExpr
			{ x = new ExprBinary(x.getContext(), ExprBinary.BINOP_AND, x, r); }
		)*
	;

bitwiseExpr returns [Expression x] { x = null; Expression r; int o = 0; }
	:	x=equalExpr
		(	( BITWISE_OR  { o = ExprBinary.BINOP_BOR; }
			| BITWISE_AND { o = ExprBinary.BINOP_BAND; }
			| BITWISE_XOR { o = ExprBinary.BINOP_BXOR; }
			)
			r=equalExpr
			{ x = new ExprBinary(x.getContext(), o, x, r); }
		)*
	;

equalExpr returns [Expression x] { x = null; Expression r; int o = 0; }
	:	x=compareExpr
		(	( EQUAL     { o = ExprBinary.BINOP_EQ; }
			| NOT_EQUAL { o = ExprBinary.BINOP_NEQ; }
			)
			r = compareExpr
			{ x = new ExprBinary(x.getContext(), o, x, r); }
		)*
	;

compareExpr returns [Expression x] { x = null; Expression r; int o = 0; }
	:	x=addExpr
		(	( LESS_THAN  { o = ExprBinary.BINOP_LT; }
			| LESS_EQUAL { o = ExprBinary.BINOP_LE; }
			| MORE_THAN  { o = ExprBinary.BINOP_GT; }
			| MORE_EQUAL { o = ExprBinary.BINOP_GE; }
			)
			r = addExpr
			{ x = new ExprBinary(x.getContext(), o, x, r); }
		)*
	;

addExpr returns [Expression x] { x = null; Expression r; int o = 0; }
	:	x=multExpr
		(	( PLUS  { o = ExprBinary.BINOP_ADD; }
			| MINUS { o = ExprBinary.BINOP_SUB; }
			)
			r=multExpr
			{ x = new ExprBinary(x.getContext(), o, x, r); }
		)*
	;

multExpr returns [Expression x] { x = null; Expression r; int o = 0; }
	:	x=castExpr
		(	( STAR { o = ExprBinary.BINOP_MUL; }
			| DIV  { o = ExprBinary.BINOP_DIV; }
			| MOD  { o = ExprBinary.BINOP_MOD; }
			)
			r=castExpr
			{ x = new ExprBinary(x.getContext(), o, x, r); }
		)*
	;

castExpr returns [Expression x] { x = null; Type t=null; }
	:	(LPAREN primitive_type) =>
		  (l:LPAREN t=primitive_type RPAREN) x=inc_dec_expr
		{ x = new ExprTypeCast(getContext(l), t, x); }
	|	x=inc_dec_expr
	;

inc_dec_expr returns [Expression x] { x = null; }
	:	(incOrDec) => x=incOrDec
	|	b:BANG x=value_expr { x = new ExprUnary(getContext(b),
												ExprUnary.UNOP_NOT, x); }
	|	x=value_expr
	;

incOrDec returns [Expression x] { x = null; }
	:	x=left_expr
		(	INCREMENT
			{ x = new ExprUnary(x.getContext(), ExprUnary.UNOP_POSTINC, x); }
		|	DECREMENT
			{ x = new ExprUnary(x.getContext(), ExprUnary.UNOP_POSTDEC, x); }
		)
	|	i:INCREMENT x=left_expr
			{ x = new ExprUnary(getContext(i), ExprUnary.UNOP_PREINC, x); }
	|	d:DECREMENT x=left_expr
			{ x = new ExprUnary(getContext(d), ExprUnary.UNOP_PREDEC, x); }
	;

value_expr returns [Expression x] { x = null; boolean neg = false; }
	:	(m:MINUS { neg = true; })?
		(x=minic_value_expr | x=streamit_value_expr)
		{ if (neg) x = new ExprUnary(getContext(m), ExprUnary.UNOP_NEG, x); }
	;

streamit_value_expr returns [Expression x] { x = null; }
	:	t:TK_pop LPAREN RPAREN
			{ x = new ExprPop(getContext(t)); }
	|	u:TK_peek LPAREN x=right_expr RPAREN
			{ x = new ExprPeek(getContext(u), x); }
	;

minic_value_expr returns [Expression x] { x = null; }
	:	LPAREN x=right_expr RPAREN
	|	(func_call) => x=func_call
	|	x=value
	|	x=constantExpr
	;

value returns [Expression x] { x = null; Expression array; }
	:	name:ID { x = new ExprVar(getContext(name), name.getText()); }
		(	DOT field:ID
			{ x = new ExprField(x.getContext(), x, field.getText()); }
		|	l:LSQUARE

			(array=right_expr { x = new ExprArray(x.getContext(), x, array); }
			| { throw new SemanticException("missing array index",
						getFilename(), l.getLine()); }
			)
			RSQUARE
		)*
	;

constantExpr returns [Expression x] { x = null; }
	:	n:NUMBER
			{ x = ExprConstant.createConstant(getContext(n), n.getText()); }
	|	c:CHAR_LITERAL
			{ x = new ExprConstChar(getContext(c), c.getText()); }
	|	s:STRING_LITERAL
			{ x = new ExprConstStr(getContext(s), s.getText()); }
	|	pi:TK_pi
			{ x = new ExprConstFloat(getContext(pi), Math.PI); }
	|	t:TK_true
			{ x = new ExprConstBoolean(getContext(t), true); }
	|	f:TK_false
			{ x = new ExprConstBoolean(getContext(f), false); }
	;

struct_decl returns [TypeStruct ts]
{ ts = null; Parameter p; List names = new ArrayList();
	List types = new ArrayList(); }
	:	t:TK_struct id:ID
		LCURLY
		(p=param_decl SEMI
			{ names.add(p.getName()); types.add(p.getType()); }
		)*
		RCURLY
		{ ts = new TypeStruct(getContext(t), id.getText(), names, types); }
	;
