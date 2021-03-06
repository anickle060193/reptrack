package com.adamnickle.reptrack.model.message

class AccelerometerMessage( map: Map<String, Any> )
{
    val type: String by map
    val workoutId: Long by map
    val exerciseId: Long by map
    val exerciseSetId: Long by map
    val x: List<Float> by map
    val y: List<Float> by map
    val z: List<Float> by map
    val time: Long by map
    val sampleRate: Int by map
    val period: Int by map
}