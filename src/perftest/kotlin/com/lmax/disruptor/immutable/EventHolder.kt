package com.lmax.disruptor.immutable

import com.lmax.disruptor.EventFactory

class EventHolder {

    var event: SimpleEvent? = null

    companion object {

        val FACTORY: EventFactory<EventHolder> = EventFactory { EventHolder() }
    }
}
