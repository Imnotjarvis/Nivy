package com.nivy.data.model.sync

interface Identifiable<ID : UniqueId> {
    val id: ID
}
