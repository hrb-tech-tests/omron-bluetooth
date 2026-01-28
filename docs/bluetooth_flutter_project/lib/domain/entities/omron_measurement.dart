class OmronMeasurement {
  final double systolic;
  final double diastolic;
  final double meanArterialPressure;
  final DateTime timestamp;
  final double pulseRate;
  final int userId;

  OmronMeasurement({
    required this.systolic,
    required this.diastolic,
    required this.meanArterialPressure,
    required this.timestamp,
    required this.pulseRate,
    required this.userId,
  });

  Map<String, dynamic> toJson() {
    return {
      'systolic': systolic,
      'diastolic': diastolic,
      'meanArterialPressure': meanArterialPressure,
      'timestamp': timestamp.toIso8601String(),
      'pulseRate': pulseRate,
      'userId': userId,
    };
  }
}
