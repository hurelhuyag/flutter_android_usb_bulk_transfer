/// Error-diffusion algorithms that simulate greys and colours with patterns of black dots.
enum DitheringAlgorithm { none, floydSteinberg, atkinson }

/// How `writePdf` reproduces colours and greys that are neither black nor white.
final class Dithering {
  /// Solid black and white: each dot is decided by the threshold alone. Best for logos, QR codes and text.
  static const none = Dithering._(DitheringAlgorithm.none, 0);

  /// Floyd–Steinberg error diffusion: each dot's rounding error goes to 4 neighbours. Smooth, fine-grained greys that
  /// keep even very light tints.
  ///
  /// [strength] (0-1) is the fraction of the error that is spread. 1 is the standard algorithm; lower values keep
  /// more of the image solid, for example shaded logos at 0.5.
  const Dithering.floydSteinberg({double strength = 1.0})
      : assert(strength >= 0 && strength <= 1, 'strength must be between 0 and 1'),
        algorithm = DitheringAlgorithm.floydSteinberg,
        strength = strength;

  /// Atkinson error diffusion: 6/8 of each dot's rounding error goes to 6 neighbours over two rows. Higher contrast
  /// than Floyd–Steinberg: dark colours stay solid black and light tints print as sparser dots.
  ///
  /// [strength] (0-1) is the fraction of the error that is spread. 1 is the standard algorithm.
  const Dithering.atkinson({double strength = 1.0})
      : assert(strength >= 0 && strength <= 1, 'strength must be between 0 and 1'),
        algorithm = DitheringAlgorithm.atkinson,
        strength = strength;

  const Dithering._(this.algorithm, this.strength);

  final DitheringAlgorithm algorithm;
  final double strength;

  @override
  String toString() => 'Dithering.${algorithm.name}(strength: $strength)';
}
