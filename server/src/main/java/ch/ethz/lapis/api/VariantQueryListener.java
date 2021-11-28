package ch.ethz.lapis.api;

import ch.ethz.lapis.api.entity.AAMutation;
import ch.ethz.lapis.api.entity.NucMutation;
import ch.ethz.lapis.api.entity.query.*;
import ch.ethz.lapis.api.parser.VariantQueryBaseListener;
import ch.ethz.lapis.api.parser.VariantQueryParser;

import java.util.Stack;


public class VariantQueryListener extends VariantQueryBaseListener {

    private Stack<VariantQueryExpr> exprStack = new Stack<>();

    public VariantQueryListener() {
        exprStack.push(new Single());
    }

    @Override
    public void enterAnd(VariantQueryParser.AndContext ctx) {
        VariantQueryExpr expr = new BiOp(BiOp.OpType.AND);
        exprStack.peek().putValue(expr);
        exprStack.push(expr);
    }

    @Override
    public void enterOr(VariantQueryParser.OrContext ctx) {
        VariantQueryExpr expr = new BiOp(BiOp.OpType.OR);
        exprStack.peek().putValue(expr);
        exprStack.push(expr);
    }

    @Override
    public void enterNeg(VariantQueryParser.NegContext ctx) {
        VariantQueryExpr expr = new Negation();
        exprStack.peek().putValue(expr);
        exprStack.push(expr);
    }

    @Override
    public void exitNeg(VariantQueryParser.NegContext ctx) {
        exprStack.pop();
    }

    @Override
    public void exitAnd(VariantQueryParser.AndContext ctx) {
        exprStack.pop();
    }

    @Override
    public void exitOr(VariantQueryParser.OrContext ctx) {
        exprStack.pop();
    }

    @Override
    public void enterPango_query(VariantQueryParser.Pango_queryContext ctx) {
        PangoQuery pangoQuery = new PangoQuery(
            ctx.pango_lineage().getText(),
            ctx.pango_include_sub() != null
        );
        exprStack.peek().putValue(pangoQuery);
    }

    @Override
    public void enterNextstrain_clade_query(VariantQueryParser.Nextstrain_clade_queryContext ctx) {
        NextstrainClade nextstrainClade = new NextstrainClade(ctx.nextstrain_clade().getText());
        exprStack.peek().putValue(nextstrainClade);
    }

    @Override
    public void enterGisaid_clade_query(VariantQueryParser.Gisaid_clade_queryContext ctx) {
        GisaidClade gisaidClade = new GisaidClade(ctx.gisaid_clade().getText());
        exprStack.peek().putValue(gisaidClade);
    }

    @Override
    public void enterAa_mut(VariantQueryParser.Aa_mutContext ctx) {
        AAMutation aaMutation = new AAMutation(
            ctx.gene().getText(),
            Integer.parseInt(ctx.position().getText()),
            ctx.aa_mutated() != null ? ctx.aa_mutated().getText().charAt(0) : null
        );
        exprStack.peek().putValue(aaMutation);
    }

    @Override
    public void enterNuc_mut(VariantQueryParser.Nuc_mutContext ctx) {
        NucMutation nucMutation = new NucMutation(
            Integer.parseInt(ctx.position().getText()),
            ctx.nuc_mutated() != null ? ctx.nuc_mutated().getText().charAt(0) : null
        );
        exprStack.peek().putValue(nucMutation);
    }

    public VariantQueryExpr getExpr() {
        return exprStack.peek();
    }
}
