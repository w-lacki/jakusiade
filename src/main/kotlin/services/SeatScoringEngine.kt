package pl.wiktorlacki.services

import pl.wiktorlacki.models.Seat
import java.util.PriorityQueue

class SeatScoringEngine {

    companion object {
        private val COMPARATOR = compareBy<SeatScore> { it.duration }
    }

    data class SeatScore(
        val seat: Seat,
        val duration: Long,
        val startIndex: Int,
        val endIndex: Int
    )

    fun calculateTopSeats(
        seatMap: Map<Seat, List<Int>>,
        segmentsDurations: List<Long>,
        limit: Int = 3
    ): List<SeatScore> {
        val top = PriorityQueue<SeatScore>(COMPARATOR)

        seatMap.forEach { (seat, segment) ->
            var currentStart = segment[0]
            var currentDuration = segmentsDurations[currentStart]

            fun recordSegment(duration: Long, start: Int, end: Int) {
                val candidate = SeatScore(seat, duration, start, end + 1)
                if (top.size < limit) {
                    top.add(candidate)
                } else if (duration > (top.peek()?.duration ?: 0L)) {
                    top.poll()
                    top.add(candidate)
                }
            }

            segment.windowed(size = 2).forEach { (prev, curr) ->
                if (prev + 1 == curr) {
                    currentDuration += segmentsDurations[curr]
                } else {
                    recordSegment(currentDuration, currentStart, prev)
                    currentStart = curr
                    currentDuration = segmentsDurations[currentStart]
                }
            }

            recordSegment(currentDuration, currentStart, segment.last())
        }

        return top.sortedByDescending { it.duration }
    }
}