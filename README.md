# Java concurrency and networking

Educational Java components developed by Ivan Davydov for the Java Advanced
course by Georgiy Korneev.

## Included components

- An RMI-backed bank model with thread-safe account updates.
- A non-blocking UDP echo server built with `DatagramChannel` and `Selector`.
- A fixed-size parallel mapper that preserves input order.

The RMI component is not an implementation of a custom TCP protocol. The
networking component in this repository uses UDP.

## Build and test

Requirements: Java 21 and Maven 3.9+.

```sh
mvn test
```

## Attribution

The original implementations were coursework submissions. This repository
contains an adapted, standalone subset of Ivan Davydov's own solutions; it
does not include course libraries, task interfaces, test suites, scripts, or
documentation.

The code in this repository is distributed under the [MIT License](LICENSE).
