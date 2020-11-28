# invoice-generator

> :information_source: Requires **JDK 8** or newer

## What it generates?

![Generated PDF](/pdf.png)

## How to execute it?

1. `make build-jar`
    
    * :information_source: Rebuilds the executable (`.jar`) file from project sources
2. `make make-pdf LANG=EN,DE`
    
    * :information_source: Runs the executable to produce PDF(s) in desired language(s). PDFs are generated in the project root
    * You can specify languages as comma delimited list, examples: `EN` or `EN,DE`
    
## Use own data

* Adjust `src/main/resources/data-template.yml` to use own data
* Adjust `src/main/resources/translations.yml` to change translations