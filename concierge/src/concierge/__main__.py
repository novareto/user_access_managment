"""Command-line interface."""
import click


@click.command()
@click.version_option()
def main() -> None:
    """Concierge."""


if __name__ == "__main__":
    main(prog_name="concierge")  # pragma: no cover
