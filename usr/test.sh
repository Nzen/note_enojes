#! /bin/sh

# prep
cp org/eno_lang/locale/Analysis_de.properties .
mv org/eno_lang/locale/Loaders_de.properties org/eno_lang/locale/Loaders_de.propertiesf
	# ensures there's no confution between property loaded and List loaded
javac org/eno_lang/locale/Loaders_de.java
javac org/eno_lang/locale/TestLocaleFiles.java
# actual test should show all the keys and messages
java -cp .:org.eno_lang.locale org.eno_lang.locale.TestLocaleFiles
# cleanup
mv org/eno_lang/locale/Loaders_de.propertiesf org/eno_lang/locale/Loaders_de.properties
rm Analysis_de.properties

