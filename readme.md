
### Note Enojes

Note Enojes is a library to convert eno lang locale files to ListResourceBundle java classes ready for use with java MessageFormatter. Uses Java v8 syntax, includes a Java v9 module descriptor. (Incomplete)

Eno is a document / data structure specification. Its authors provide gettext compatible files through the [eno-locales](https://github.com/eno-lang/eno-locales) repository.

"Note Enojes" is a misspellt spanish phrase telling (informal)you not to get angry. I chose it partly as reference to the Enohar library, and because note is some text in english, which is relevant for translating text.

&copy; Nicholas Prado. Begun 2018. Released under MIT license terms.

#### Maven pom dependence declaration

```xml
		<dependency>
			<groupId>
				ws.nzen.format
			</groupId>
			<artifactId>
				note_enojes
			</artifactId>
			<version>
				1.0
			</version>
		</dependency>
```

This will require Apache's commons cli library.

It also expects the inclusion of two resource bundle ready files for providing localized error messages. Uses the default jvm locale to select which. This project currently includes english and (weak) spanish translations.

#### Usage

Note_enojes offers a cli interface. It assumes a user will prepare session arguments in a properties file. One can point to the file by providing the "-c" argument, followed by a filepath, ex -c "config/f.txt". Here follows an example properties body:

```
input_directory = etc

output_directory = usr

separate_by_category = true

resource_bundle_style = properties

output_into_package = true

java_package = org.eno-lang.locale

replace_variables = true

list_template_file = src/main/resources/template_lrb_class_upper.txt
```

* input_directory is a path to find the gettext po files prepared by eno-locale
* output_directory is a path to emit the resulting files to; falls back to input if output is invalid
* separate\_by_category whether to create a file per group or one per language
* resource\_bundle_style {list , properties} the style of file to emit: a properties file or a ListResourceBundle
* output\_into\_package whether the resulting files should be in subfolders from output corresponding to \_package_
* java_package is a java package one may want to copy the files to; if bundle style is list, the classes will use this package
* replace_variables indicates to replace variables like [MAXIMUM] with an escape string for MessageFormat output, ex {1,number} .
* list\_template\_file The location of a template file for starting ListResourceBundle classes







































