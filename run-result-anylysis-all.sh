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
./run-result-anylysis.sh ToDoList $OUTPUTFILE
./run-result-anylysis.sh Zxing $OUTPUTFILE
./run-result-anylysis.sh Music $OUTPUTFILE
./run-result-anylysis.sh MyTracks_1 $OUTPUTFILE
./run-result-anylysis.sh Browser $OUTPUTFILE
./run-result-anylysis.sh ConnectBot $OUTPUTFILE
./run-result-anylysis.sh FireFox $OUTPUTFILE

# Test
./run-result-anylysis.sh SoundRecorder $OUTPUTFILE
./run-result-anylysis.sh Swiftnotes $OUTPUTFILE
./run-result-anylysis.sh PhotoAffix $OUTPUTFILE
./run-result-anylysis.sh MLManager $OUTPUTFILE
./run-result-anylysis.sh InstaMaterial $OUTPUTFILE
./run-result-anylysis.sh Tomdroid $OUTPUTFILE
./run-result-anylysis.sh SGTPuzzles $OUTPUTFILE
./run-result-anylysis.sh Aard $OUTPUTFILE
./run-result-anylysis.sh ClipStack $OUTPUTFILE
./run-result-anylysis.sh KissLauncher $OUTPUTFILE
./run-result-anylysis.sh DashClock $OUTPUTFILE
./run-result-anylysis.sh Dns66 $OUTPUTFILE
./run-result-anylysis.sh CleanMaster $OUTPUTFILE
./run-result-anylysis.sh OmniNotes $OUTPUTFILE
./run-result-anylysis.sh Solitaire $OUTPUTFILE
./run-result-anylysis.sh Mms $OUTPUTFILE
./run-result-anylysis.sh MyTracks_2 $OUTPUTFILE
./run-result-anylysis.sh MiMangaNu $OUTPUTFILE
./run-result-anylysis.sh QKSMS $OUTPUTFILE
./run-result-anylysis.sh K9Mail $OUTPUTFILE

rm $ResultAnalysisJarDir/libbuddy.so
