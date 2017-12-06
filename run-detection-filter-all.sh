# Compile
cd DetectionFilterJar/chord-src-2.0
ant clean
ant compile
cd ../..

# Train
./run-detection-filter.sh Music.apk false 2 $PWD"/Result/Train"
./run-detection-filter.sh ConnectBot.apk false 1 $PWD"/Result/Train"
./run-detection-filter.sh ZXing.apk false 1 $PWD"/Result/Train"
./run-detection-filter.sh ToDoList.apk false 1 $PWD"/Result/Train"
./run-detection-filter.sh Browser.apk false 2 $PWD"/Result/Train"
./run-detection-filter.sh MyTracks_1.apk false 1 $PWD"/Result/Train"
./run-detection-filter.sh Firefox.apk false 1 $PWD"/Result/Train"

# Test
./run-detection-filter.sh Aard.apk false 3 $PWD"/Result/Test"
./run-detection-filter.sh K9Mail.apk false 3 $PWD"/Result/Test"
./run-detection-filter.sh Mms.apk false 3 $PWD"/Result/Test"
./run-detection-filter.sh Mytracks_2.apk false 3 $PWD"/Result/Test"
./run-detection-filter.sh SGTPuzzles.apk false 3 $PWD"/Result/Test"
./run-detection-filter.sh Tomdroid.apk false 3 $PWD"/Result/Test"
./run-detection-filter.sh ClipStack.apk false 3 $PWD"/Result/Test"
./run-detection-filter.sh DashClock.apk false 3 $PWD"/Result/Test"
./run-detection-filter.sh KissLauncher.apk false 3 $PWD"/Result/Test"
./run-detection-filter.sh QKSMS.apk false 1 $PWD"/Result/Test"
./run-detection-filter.sh Swiftnotes.apk false 1 $PWD"/Result/Test"
./run-detection-filter.sh InstaMaterial.apk false 1 $PWD"/Result/Test"
./run-detection-filter.sh MLManager.apk false 3 $PWD"/Result/Test"
./run-detection-filter.sh OmniNotes.apk false 3 $PWD"/Result/Test"
./run-detection-filter.sh PhotoAffix.apk false 3 $PWD"/Result/Test"
./run-detection-filter.sh SoundRecorder.apk false 1 $PWD"/Result/Test"
./run-detection-filter.sh CleanMaster.apk false 1 $PWD"/Result/Test"
./run-detection-filter.sh MiMangaNu.apk false 3 $PWD"/Result/Test"
./run-detection-filter.sh Dns66.apk false 3 $PWD"/Result/Test"
./run-detection-filter.sh Solitaire.apk false 1 $PWD"/Result/Test"

# Injected
./run-detection-filter.sh Aard-Injected.apk false 3 $PWD"/Result/Injected"
./run-detection-filter.sh Tomdroid-Injected.apk false 3 $PWD"/Result/Injected"
./run-detection-filter.sh Browser-Injected.apk false 4 $PWD"/Result/Injected"
./run-detection-filter.sh Mms-Injected.apk false 3 $PWD"/Result/Injected"
./run-detection-filter.sh Music-Injected.apk false 4 $PWD"/Result/Injected"
./run-detection-filter.sh SGTPuzzles-Injected.apk false 3 $PWD"/Result/Injected"
./run-detection-filter.sh Mytracks-Injected.apk false 3 $PWD"/Result/Injected"
./run-detection-filter.sh K9-Injected.apk false 3 $PWD"/Result/Injected"

# Clean
cd DetectionFilterJar/chord-src-2.0
ant clean
cd ../..
