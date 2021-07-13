/*
@author Arthur Godet <arth.godet@gmail.com>
@since 12/02/2021
*/

package main;

import alldifferentprec.AllDiffPrec;
import alldifferentprec.AllDiffPrecMoreThanBc;
import alldifferentprec.GreedyBoundSupport;
import alldifferentprec.PropAllDiffPrec;
import data.Factory;
import data.PrecInstance;
import java.util.ArrayList;
import java.util.Arrays;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.nary.alldifferent.PropAllDiffBC;
import org.chocosolver.solver.constraints.nary.alldifferent.PropAllDiffInst;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;

public class PrecModel {
    private final Model model;

    public PrecModel(PrecInstance instance, ConfigurationPrec configuration) {
        this.model = new Model();
        IntVar[] vars = new IntVar[instance.getSize()];
        for(int k = 0; k < vars.length; k++) {
            vars[k] = model.intVar("vars["+k+"]", instance.getDomains()[k]);
        }

        IntVar obj = model.intVar("obj",
                                  Arrays.stream(vars).mapToInt(IntVar::getLB).min().getAsInt(),
                                  Arrays.stream(vars).mapToInt(IntVar::getUB).max().getAsInt()
        );
        model.max(obj, vars).post();
        model.setObjective(false, obj);

        ArrayList<Propagator<IntVar>> list = new ArrayList<>();
        list.add(new PropAllDiffInst(vars));
        boolean[][] precedence = PropAllDiffPrec.buildPrecedence(instance.getAncestors(), instance.getDescendants(), true);
        if(configuration.equals(ConfigurationPrec.DECOMPOSITION)) {
            for(int i = 0; i < precedence.length; i++) {
                for(int j = 0; j < precedence.length; j++) {
                    if(i != j && precedence[i][j]) {
                        model.arithm(vars[i], "<", vars[j]).post();
                    }
                }
            }
            list.add(new PropAllDiffBC(vars));
        } else if(configuration.equals(ConfigurationPrec.BESSIERE)) {
            list.add(new PropAllDiffPrec(vars, precedence, new AllDiffPrec(vars, precedence)));
        } else if(configuration.equals(ConfigurationPrec.GODET_BC)) {
            list.add(new PropAllDiffPrec(vars, precedence, new AllDiffPrecMoreThanBc(vars, precedence, false)));
        } else if(configuration.equals(ConfigurationPrec.GODET_RC)) {
            list.add(new PropAllDiffPrec(vars, precedence, new AllDiffPrecMoreThanBc(vars, precedence, true)));
        } else if(configuration.equals(ConfigurationPrec.GREEDY_BC)) {
            list.add(new PropAllDiffPrec(vars, precedence, new GreedyBoundSupport(vars, precedence, false)));
        } else if(configuration.equals(ConfigurationPrec.GREEDY_RC)) {
            list.add(new PropAllDiffPrec(vars, precedence, new GreedyBoundSupport(vars, precedence, true)));
        }

        model.post(
            new Constraint(
                "ORDER_CONSTRAINT",
                list.toArray(new Propagator[0])
            )
        );

        model.getSolver().setSearch(
//            Search.inputOrderUBSearch(vars),
            Search.inputOrderLBSearch(vars),
//            Search.intVarSearch(
//                new FirstFail(model),
//                new IntDomainMin(),
//                vars
//            ),
            Search.inputOrderLBSearch(obj)
        );
    }

    public Model getModel() {
        return model;
    }

    public static String toString(Solver solver, boolean finalStats) {
        return (finalStats ?
                solver.getMeasures().getTimeToBestSolutionInNanoSeconds() :
                solver.getMeasures().getTimeCountInNanoSeconds()
        ) / 1000000 + ";"
            + solver.getBestSolutionValue().intValue() + ";"
            + solver.getNodeCount() + ";"
            + solver.getBackTrackCount() + ";"
            + solver.getFailCount() + ";";
    }

    public static void main(String[] args) {
        ConfigurationPrec configuration = ConfigurationPrec.valueOf(args[0]);
        long timeLimitInMilliseconds = Long.parseLong(args[1]) * 60000;
        PrecInstance instance = Factory.fromFile(args[2], PrecInstance.class);
        assert instance != null;
        PrecModel precModel = new PrecModel(instance, configuration);
        Solver solver = precModel.getModel().getSolver();

        solver.limitTime(timeLimitInMilliseconds);
        while(solver.solve()) {
            System.out.println(toString(solver, false));
        }
        System.out.println(
            instance.getName() + ";"
                + solver.getTimeCountInNanoSeconds() / 1000000 + ";"
                + toString(solver, true)
        );
    }
}
