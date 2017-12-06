# Modeling #############################################
# Train
./run-model.sh ConnectBot.apk false
# Test
./run-model.sh Aard.apk true
# Injected
./run-model.sh Aard-Injected.apk true
#######################################################



# Detection and Filter ################################
# Compile
cd DetectionFilterJar/chord-src-2.0
ant clean
ant compile
cd ../..

# Train
./run-detection-filter.sh ConnectBot.apk false 1 $PWD"/Result/Train"
# Test
./run-detection-filter.sh Aard.apk false 3 $PWD"/Result/Test"
# Injected
./run-detection-filter.sh Aard-Injected.apk false 3 $PWD"/Result/Injected"

# Clean
cd DetectionFilterJar/chord-src-2.0
ant clean
cd ../..
#######################################################



# Result Analysis######################################
CHORD_DIR=$PWD"/DetectionFilterJar/chord-src-2.0"
RESULTDIR=$PWD"/Result"
ResultAnalysisJarDir=$PWD"/ResultAnalysisJar"
cd $CHORD_DIR/bdd
make
cd ../../..
mv $CHORD_DIR/libbuddy.so $ResultAnalysisJarDir
cp $ResultAnalysisJarDir/Template.csv $RESULTDIR/ResultAnalysis.csv
OUTPUTFILE=$RESULTDIR/ResultAnalysis.csv

# Train
./run-result-anylysis.sh ConnectBot $OUTPUTFILE
# Test
./run-result-anylysis.sh Aard $OUTPUTFILE

rm $ResultAnalysisJarDir/libbuddy.so
#######################################################
