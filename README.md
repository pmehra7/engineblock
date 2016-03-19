## The Empirical Machine

This project aims to provide a missing power tool in the test tooling arsenal.

The design goals:

1. Provide a useful and intuitive Reusable Machine Pattern for constructing and reasoning about concurrent performance tests. To encourage this, the runtime machinery is based on [simple and tangible core concepts](docs/core_concepts.md).
2. Reduce testing time of complex scenarios with many variables. This is achieved by controlling tests from an [open javascript sandbox](docs/scripting.md). This makes more sophisticated scenarios possible when needed.
3. Minimize the amount of effort required to get empirical results from a test cycle. For this, [metrics reporting](docs/metrics.md) is baked in.
4. Encourage enhancement through community contributions. We hope this

This is simply a matter of using the best tooling available to bring data together in a common view. In other words, metrics and dashboards. Empirical Machine supports reporting to common metrics systems out of the box.

In short, Empirical Machine wishes to be a programmable power tool for performance testing.

## Scale

For now, this is a single-instance client. For testing large clusters, you will still need to run multiple clients to provide adequate loading. Experience has shown that one client can adequately drive 3-5 target systems for typical workloads. As always, be sure to watch your metrics on both sides to ensure that your testing instrument isn't the thing being measured.

## Getting Started

You can begin at [Quick Start](docs/quickstart.md) or consult the full [Users Guide](docs/usersguide.md). At least taking a glance at the user guide is recommended.
 
If you are interested in contributing to Empirical Machine, more information is available in the [Developer's section](doc/developers.md)

## History

The Empirical Machine is an evolution of [test client](http://github.com/jshook/testclient). That project is no longer in active development.

## License

This is licensed under the Apache Public License 2.0


