# genera

A low-level multiplatform collection of utilities for building loops, intended to support re-creating [Mobius](https://github.com/spotify/mobius)
as a modernised multiplatform library with a few more degrees of freedom. 


TODO:
----

- consider using just Flows or Channels, not Connectable. Basically all-in on coroutines
- add iOS code/Swift wrapper (get help!)
- consider more specific types: thread-safe, one-to-one (one item in == one item out), stateful
- consider some other way than switching to a different Dispatcher to manage thread-safety. Maybe reducing
  parallelism is enough; assuming that visibility issues are handled by coroutines. I think they should be.