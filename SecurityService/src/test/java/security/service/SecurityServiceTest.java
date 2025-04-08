package security.service;

import image.service.ImageService;
import security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private SecurityService securityService;

    private Sensor testSensor;

    @BeforeEach
    void initializeTestSensor() {
        testSensor = new Sensor("Test Sensor", SensorType.DOOR);
    }

    @Test
    void shouldSetPendingAlarmWhenSensorActivatedWhileArmed() {
        // Setup alarm status and activate sensor
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(testSensor, true);
        
        // Verify alarm status was updated to pending
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void shouldTriggerAlarmWhenSensorActivatedWhilePending() {
        // Create and set up sensor with pending alarm
        Sensor doorSensor = new Sensor("Test Sensor", SensorType.DOOR);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(Set.of(doorSensor));
        
        // Activate sensor and verify alarm is triggered
        securityService.changeSensorActivationStatus(doorSensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void shouldResetToNoAlarmWhenAllSensorsInactiveWhilePending() {
        // Setup active sensor and pending alarm state
        testSensor.setActive(true);
        Set<Sensor> activeSensors = new HashSet<>();
        activeSensors.add(testSensor);
        
        when(securityRepository.getSensors()).thenReturn(activeSensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        
        // Deactivate sensor and verify alarm resets
        securityService.changeSensorActivationStatus(testSensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void shouldNotChangeAlarmStateWhenAlarmIsActive() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(testSensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void shouldTriggerAlarmWhenActiveSensorReactivatedWhilePending() {
        Sensor sensor = new Sensor("Test Sensor", SensorType.DOOR);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor));
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void shouldNotChangeStateWhenInactiveSensorDeactivated() {
        testSensor.setActive(false);
        securityService.changeSensorActivationStatus(testSensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void shouldTriggerAlarmWhenCatDetectedAndSystemArmedHome() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(image);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void shouldResetToNoAlarmWhenNoCatDetectedAndAllSensorsInactive() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(Set.of(testSensor));
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(image);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"ALARM", "PENDING_ALARM"})
    void shouldResetToNoAlarmWhenSystemDisarmed(AlarmStatus alarmStatus) {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void shouldResetAllSensorsToInactiveWhenSystemArmed(ArmingStatus status) {
        testSensor.setActive(true);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(testSensor);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(status);
        
        // Verify sensor was deactivated
        verify(securityRepository).updateSensor(testSensor);
        assertFalse(testSensor.getActive(), "Sensor should be inactive after system is armed");
    }

    @Test
    void shouldSetAlarmStatusToAlarmWhenArmedHomeWithCatPresent() {
        // Setup image with cat
        BufferedImage catImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        
        // Process cat image while system is disarmed
        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.DISARMED)
                .thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(catImage);
        
        // Change to ARMED_HOME and verify alarm is triggered
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
}