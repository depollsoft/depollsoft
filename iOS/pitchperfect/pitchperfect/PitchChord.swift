/// Names the chord the barbershop community lives on. Cells are chromatic
/// steps from the range root (cell 12 is the root's octave); voicing and
/// doubled roots do not matter, only the pitch classes sounding together.
enum PitchChord {
    static let barbershopSeventh = "BARBERSHOP!"

    /// Root, major third, perfect fifth, minor seventh: the dominant seventh.
    private static let dominantSeventh: Set<Int> = [0, 4, 7, 10]

    static func name(cells: [Int]) -> String? {
        let pitchClasses = Set(cells.map { (($0 % 12) + 12) % 12 })
        guard pitchClasses.count == dominantSeventh.count else { return nil }
        let isSeventh = pitchClasses.contains { root in
            Set(pitchClasses.map { ($0 - root + 12) % 12 }) == dominantSeventh
        }
        return isSeventh ? barbershopSeventh : nil
    }
}
