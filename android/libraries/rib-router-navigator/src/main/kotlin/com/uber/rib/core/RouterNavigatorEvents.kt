/*
 * Copyright (C) 2017. Uber Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.uber.rib.core

import io.reactivex.Observable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.rx2.asObservable

/** Class that provides its instance to emit or subscribe to [RouterNavigatorEvent]  */
class RouterNavigatorEvents private constructor() {

  private val _events = MutableSharedFlow<RouterNavigatorEvent>(0, 1, BufferOverflow.DROP_OLDEST)

  /** @return the stream which can be subcribed to listen for [RouterNavigatorEvent] */
  val events: Observable<RouterNavigatorEvent> = _events.asObservable()

  /**
   * Emits a new [RouterNavigatorEvent] on the stream.
   *
   * @param eventType type of the navigation event.
   * @param parent router instance to which child will attach to.
   * @param child router instance which getting attached.
   */
  fun emitEvent(eventType: RouterNavigatorEventType, parent: Router<*>, child: Router<*>) {
    _events.tryEmit(RouterNavigatorEvent(eventType, parent, child))
  }

  companion object {
    /** @return the singleton instance */
    @JvmStatic
    val instance = RouterNavigatorEvents()
  }
}
