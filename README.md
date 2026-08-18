# checkstyle-files-generator

`checkstyle-files-generator` is Checkstyle's build-time command-line application for
generating XML metadata and converting XDoc `.xml.template` files into generated
`.xml` pages. It is not an end-user Checkstyle distribution; it is developed alongside
the main [Checkstyle repository](https://github.com/checkstyle/checkstyle).

## Build and verify

Run:

```bash
./mvnw clean verify
```

## Developing with Checkstyle

After changing this project, install its current snapshot locally:

```bash
./mvnw clean install
```

For a local integration build, temporarily set `checkstyle-files-generator.version` in
the Checkstyle checkout's `pom.xml` to this project's version. Do not commit that
local override. Then run the usual Checkstyle
build from the `checkstyle` checkout. Checkstyle invokes this generator with
`exec-maven-plugin`. The generator jar deliberately does not bundle Checkstyle: its
runtime classpath is provided by the freshly compiled Checkstyle checkout.

Normal development is driven through the Checkstyle build rather than by invoking the
thin jar directly, because the command requires Checkstyle's build classes and dependencies.

## Releases

To publish a release, first set the release version in `pom.xml` and commit it to
`main`. Then manually run the **Release Deploy Maven Central** workflow. It checks out
`main` and deploys signed binary, source, and Javadoc artifacts to Maven Central.

The workflow does not change the version, create commits or tags, or create a GitHub
Release. It requires the Maven Central and GPG secrets configured for the repository.
