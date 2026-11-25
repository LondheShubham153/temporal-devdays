# temporal-devdays

Learn Temporal by doing: a short explanation, an exercise with a failing check, and a complete solution, in Python and in Java.

## Prerequisites
| Need | Check |
|---|---|
| [Temporal CLI](https://docs.temporal.io/cli) | `temporal --version` |
| Python 3.11+ and [uv](https://docs.astral.sh/uv/) | `python3 --version` and `uv --version` |
| or Java 21 and [Maven](https://maven.apache.org/) | `java -version` and `mvn -v` |

You only need the language you use.

## Setup
```bash
git clone https://github.com/temporal-community/temporal-devdays.git
cd temporal-devdays
temporal server start-dev      # keep running, Web UI at http://localhost:8233
```
Then check that exercise 1 runs: see the first row of the table below.

## How to use it
1. Read the concept.
2. Open `exercise_NN` in your language and finish the TODOs.
3. Run the check until it passes.
4. Compare with `solution_NN`.

A failing check at the start is expected. It tells you which TODO is next.

## Exercises
| # | Concept | Python | Java |
|---|---|---|---|
| | The first exercises land in the next pull request. | | |

## Troubleshooting
- Port 7233 in use: another dev server is running. Stop it, or use it.
- A Worker from an earlier run is still polling: stop it before starting a new one.
- Blank Web UI: upgrade the CLI (`brew upgrade temporal`).

## Contributing and license
See [CONTRIBUTING.md](CONTRIBUTING.md). MIT, see [LICENSE](LICENSE).
