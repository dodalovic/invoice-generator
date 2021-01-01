# invoice-generator

> :information_source: Requires **JDK 8** or newer

## What it generates?

![Generated PDF](/pdf.png)

## How to execute it?

* `make build-jar`
    
    * :information_source: Rebuilds the executable (`.jar`) file from project sources
* `make generate`
    
    * :information_source: Runs the executable to produce PDF(s) in desired language(s)
    
      A console will print out location of generated file(s) 

## Command line options

* `--pdf-name` or `-p`

  [OPTIONAL] Name (without `.pdf`) of file(s) to be generated

  If not specified, defaults to `$invoiceNumber-$lang.pdf`

* `--languages` or `-l`

  Comma delimited list of languages to generate invoice in, example `EN,DE`

  [OPTIONAL] Defaults to `EN`

## Examples

```shell
# Generated using sane defaults
$ make generate
# Specify generated PDF name
$ make generate ARGS="-p my-invoice-name" 
```
