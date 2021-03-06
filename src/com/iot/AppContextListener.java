package com.iot;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.codehaus.jackson.map.ObjectMapper;

import com.iot.dto.AlarmStatus;
import com.iot.dto.TemperatureLimits;
import com.iot.service.AlarmService;
import com.iot.service.TemperatureService;
import com.iot.util.SensorUtils;

import mraa.Aio;
import mraa.Dir;
import mraa.Gpio;
import mraa.Pwm;

public class AppContextListener implements ServletContextListener {
    private Timer alarmTimer;
    private Timer buzzerTimer;

    private Aio sensor;

    private Gpio buzzer;
    private Gpio red;
    private Gpio blue;
    private Gpio green;
    private Pwm led;

    private static volatile boolean canRing = false;

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        System.out.println("AppContextListener initialized.");

        // Temperature sensor
        sensor = new Aio(5);

        // Buzzer
        buzzer = new Gpio(7);
        buzzer.dir(Dir.DIR_OUT);

        // RGB LED
        red = new Gpio(9);
        green = new Gpio(11);
        blue = new Gpio(10);
        red.dir(Dir.DIR_OUT);
        green.dir(Dir.DIR_OUT);
        blue.dir(Dir.DIR_OUT);

        // LED
        led = new Pwm(6);
        led.period_ms(1);
        led.enable(true);

        red.write(0);
        green.write(0);
        blue.write(0);

        this.alarmTimer = new Timer();
        TimerTask alarmTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
        this.alarmTimer.schedule(alarmTask, 500, 500);

        this.buzzerTimer = new Timer();
        TimerTask buzzerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    if (canRing) {
                        freak();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
        this.buzzerTimer.schedule(buzzerTask, 500, 4000);
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        System.out.println("AppContextListener destroyed.");

        this.alarmTimer.cancel();
        this.buzzerTimer.cancel();
    }

    public void execute() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AlarmStatus alarmStatus = mapper.readValue(new File(AlarmService.FILE_NAME), AlarmStatus.class);

        if (alarmStatus != null && alarmStatus.isOn()) {
            double temp = SensorUtils.convertToCelsius(sensor.read());

            TemperatureLimits limits = mapper.readValue(new File(TemperatureService.FILE_NAME),
                    TemperatureLimits.class);

            if (temp < limits.getMin()) {
                red.write(0);
                green.write(0);
                blue.write(1);
                led.pulsewidth_us(10);
                canRing = true;
                System.out.println(temp + " - LOW -");
            } else if (temp >= limits.getMin() && temp <= limits.getMax()) {
                red.write(0);
                green.write(1);
                blue.write(0);
                led.pulsewidth_us(80);
                canRing = false;
                System.out.println(temp + " - MID -");
            } else {
                red.write(1);
                green.write(0);
                blue.write(0);
                led.pulsewidth_us(250);
                canRing = true;
                System.out.println(temp + " - HIGH -");
            }

        } else {
            red.write(0);
            green.write(0);
            blue.write(0);
            led.pulsewidth_us(0);
            canRing = false;
        }
    }

    private void freak() throws InterruptedException {
        ring();
        Thread.sleep(500);
        ring();
        Thread.sleep(2000);
    }

    private void ring() throws InterruptedException {
        buzzer.write(1);
        Thread.sleep(50);
        buzzer.write(0);
        Thread.sleep(50);

        buzzer.write(1);
        Thread.sleep(50);
        buzzer.write(0);
        Thread.sleep(50);

        buzzer.write(1);
        Thread.sleep(50);
        buzzer.write(0);
        Thread.sleep(50);
    }


}