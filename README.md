# invoice-generator

> :information_source: Requires **JDK 8** or newer

## What it generates?

![Generated PDF](/pdf.png)

## How to execute it?

* `make build-jar`
    
    * :information_source: Rebuilds the executable (`.jar`) file from project sources
* `make generate`
    
    * :information_source: Runs the executable to produce PDF(s) in desired language(s)

## Command line options

* `--template` or `-te`

  [OPTIONAL] An absolute path to a template file used to generate the invoice(s)
  
  If not specified, defaults to `${user.home}/invoice-generator/template.yml`
  
* `--translations` or `-tr`

  [OPTIONAL] An absolute path to a translations file used to localize the generated invoice(s)

  If not specified, defaults to `${user.home}/invoice-generator/translations.yml`
  
* `--output-dir` or `-o`

  [OPTIONAL] An absolute path to a directory (with the trailing slash) where the pdf(s) will be generated

  If not specified, defaults to `${user.home}/invoice-generator/`
  
* `--pdf-name` or `-p`

  [OPTIONAL] Name (without `.pdf`) of file(s) to be generated

  If not specified, defaults to `$invoiceNumber-$lang.pdf`

* `--languages` or `-l`

  Comma delimited list of languages to generate invoice in, example `EN,DE`

  [OPTIONAL] Defaults to `EN`

## Examples

```shell
$ make generate
$ make generate ARGS="-o /home/my-user/Desktop/ -p my-invoice-name -te /home/my-user/template-to-use.yml -tr /home/my-user/translations-to-use.yml" 
```
