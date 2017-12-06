# CGO18 nAdroid Artifact
This repository contains the artifact for the [CGO'18] paper:

*Xinwei Fu, Dongyoon Lee, and Changhee Jung. 2018. nAdroid: Statically Detecting Ordering Violations in Android Applications. In Proceedings of 2018 IEEE/ACM International Symposium on Code Generation and Optimization (CGO’18).*

### Dependency
  - java and javac 1.7 (1.8 doesn't work)
  - ant

### How to Run
```sh
$ ./run-all.sh       # run all the applications 
                     # (35 in total: 7 in train set, 20 in test set, 8 in injected set)
$ ./run-all-test.sh  # run 3 applications 
                     # (1 in train set, 1 in test set, 1 in injected set)
```
Comments:

Running all the applications costs around 30 hours in a host desktop with an 8-core cpu.
I strongly recommend to run ./run-all-test.sh first to test the environment.

### Folder Description

| Folder             | Description                                                                             |
|--------------------|-----------------------------------------------------------------------------------------|
| AndroidJar         | Contains google android libraries with different version                                |
| APKModeling        | Contains all the test applications (35 apks) and output files (java classes) of modeling|
| DetectionFilterJar | Modified Chord tool for detection and filtering.                                        |
| ModelJar           | Modified Soot tool (jar file) for modeling.                                             |
| Result             | All the results from detection and filtering.                                           |
| ResultAnalysisJar  | Jar for extracting results and generating a csv file.                                   |
| Licenses           | Flowdroid license and Chord copyright.                                                  |

 ### Result Analysis
After running ./run-all.sh or ./run-all-test.sh, a ResultAnalysis.csv file is generated in Result folder. It contains the data using in Figure 5 and Table 1 in the paper. The LOC information and manual inspection result in Table 1 are not provided in the ResultAnalysis.csv file.

The data of Table 2 exists in the Result/Injected folder. The data of Table 3 exists in the Result/Train folder. However, they all require manual inspection.

### VM with dependency installed
We also provide a VirtualBox image with all the dependency installed. Please check this [link].
   
   [CGO'18]:    <http://cgo.org/cgo2018/>
   [link]:      <https://goo.gl/V12t34>

