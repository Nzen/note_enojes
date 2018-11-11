
### Note Enojes

Note Enojes is a library to convert eno lang locale files to property or List- ResourceBundle files ready for use with java's MessageFormatter. Uses Java v8 syntax, and includes a Java v9 module descriptor. Note Enojes, outside of variable maintenance, is feature complete.

Eno is a document / data structure specification. Its authors provide a json message catalog through the [eno-locales](https://github.com/eno-lang/eno-locales) repository.

"Note Enojes" is a misspellt spanish phrase telling (informal)you not to get angry. I chose it partly as reference to the Enohar library, and because _note_ is "some text" in english, which is relevant for translation. I said it to myself a couple of times while implementing this.

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
				2.0
			</version>
		</dependency>
```

#### Gradle dependence declaration

```
	'ws.nzen.format:note_enojes:2.0'
```

This will require Apache's commons cli and Alibaba's fastjson library. It's worth noting that Note Enojes hasn't been uploaded to maven central.

It also expects the inclusion of two resource bundle ready files for providing localized error messages. Uses the default jvm locale to select which. This project currently includes english and (weak) spanish translations. The list emitting functionality also expects a template file to provide the class wrapper for each.

#### Usage

Note_enojes offers a cli interface. It assumes a user will prepare session arguments in a properties file. One can point to the file by providing the "-c" argument, followed by a filepath, ex -c "config/f.properties". Here follows an example properties body:

```
input_path = etc/messages.json

output_directory = usr

separate_by_category = true

resource_bundle_style = properties

output_into_package = true

java_package = org.eno_lang.locale

replace_variables = true

list_template_file = src/main/resources/template_lrb_class_upper.txt
```

* input_path is a path to find the messages prepared by eno-locale; appends messages.json if provided a folder rather than a file
* output_directory is a path to emit the resulting files to; falls back to input if output is invalid
* separate\_by_category whether to create a file per group or one per language
* resource\_bundle_style {list , properties} the style of file to emit: a properties file or a ListResourceBundle
* output\_into\_package whether the resulting files should be in subfolders from output corresponding to \_package_
* java_package is a java package one may want to copy the files to; if bundle style is list, the classes will use this package
* replace_variables indicates to replace variables like [MAXIMUM] with an escape string for MessageFormat output, ex {1,number}
* list\_template\_file The location of a template file for starting ListResourceBundle classes

#### Versions

Note Enojes uses compatibility versioning.

* 1.0 - Produces list and property resource bundle files.
* 1.1 - Produces an optional alias file.
* 2.0 - Uses messages.json rather than separate gettext files.






































