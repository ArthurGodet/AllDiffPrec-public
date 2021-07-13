# AllDiffPrec
This repository contains all the code that was used in the experiments in the Chapter 5 of the following thesis and in the following paper:
TODO: Add the citation of the thesis when it is fixed
TODO: Add the citation of the CP paper if accepted

## Installing

This project is a standard maven project in Java. As such an executable jar file can be built with the following command:

```
mvn clean package
```

## Running

As soon as maven has finished building the executable jar file, you can find it in the target folder. Copy and paste the jar file named allDiffPrec.jar into the home directory of this project. Finally, you can execute it to solve an instance of AllDiffPrec with the method of your choice.

```
java -jar allDiffPrec.jar ConfigurationName timeLimitInMinutes pathToInstanceJSONFile
```

For example, if you want to solve the instance at data/50/Prec_50_10_1.json with the GODET_BC approach with a 30 minutes time limit (as we use for our benchmark), you should execute the following command:

```
java -jar allDiffPrec.jar GODET_BC 30 "data/50/Prec_50_10_1.json"
```

By the end of any execution, the final line that was printed indicate the solving statistics as such:

```
instanceName;timeToProof;timeToBest;Objective;nbNodes;nbBacktracks;nbFails;
```

timeToProof and timeToBest are both expressed in milliseconds (ms). In the case of our example, the final line should look like to something like this:

```
Prec_50_10_1;194;194;174;33;65;32;
```

## Look into the code

If you want to have a look at the code, here is its packages organisation:
* **alldifferentprec**: this package contains all the code linked to the AllDiffPrec constraint (propagator, filtering algorithms, data structures).
* **data**: this package contains code useful for input/output processing, especially inside the Factory.java class.
* **main**: this package contains the code of the model composed of an AllDiffPrec constraint.

## Having a problem ?
For any encountered problem, do not hesitate to raise an issue or to directly contact me at arth.godet@gmail.com. I would be happy to answer any question with the code.
