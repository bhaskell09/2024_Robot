package frc.robot;


import java.util.Optional;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.util.HolonomicPathFollowerConfig;
import com.pathplanner.lib.util.PIDConstants;
import com.pathplanner.lib.util.ReplanningConfig;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;

import io.github.oblarg.oblog.Logger;

import frc.robot.commands.PickupPiece;
import frc.robot.commands.AutoAimShoulder;
import frc.robot.commands.ScoreAmp;
import frc.robot.commands.SetShooterVelocity;
import frc.robot.commands.SetShoulderPosition;
import frc.robot.commands.ShootGamePiece;

import frc.robot.subsystems.Drivetrain;
import frc.robot.subsystems.Feeder;
import frc.robot.subsystems.Shooter;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.Shoulder;
import frc.robot.subsystems.Vision;
import frc.robot.subsystems.swerve.DriveMotor;
import frc.robot.subsystems.Elevator;


public class RobotContainer {
  // Subsystems.
  private final Drivetrain m_drivetrain;
  private final Shooter m_shooter;
  private final Feeder m_feeder;
  private final Intake m_intake;
  private final Shoulder m_shoulder;
  private final Elevator m_elevator;
  private final Vision m_vision;

  // Controllers.
  private static final int DRIVER_CONTROLLER_PORT = 0;
  private final CommandXboxController m_driverController;

  private static final int OPERATOR_CONTROLLER_PORT = 1;
  private final CommandXboxController m_operatorController;
  
  private final SendableChooser<Command> autoChooser;
  
  public RobotContainer() {
    // Subsystem init.
    m_drivetrain = Drivetrain.getInstance();
    m_shooter = Shooter.getInstance();
    m_feeder = Feeder.getInstance();
    m_intake = Intake.getInstance();
    m_shoulder = Shoulder.getInstance();
    m_elevator = Elevator.getInstance();
    m_vision = Vision.getInstance();
  
    // PathPlanner.
    NamedCommands.registerCommand("PickupPiece", new PickupPiece());
    NamedCommands.registerCommand("AutoAimShoulder", new AutoAimShoulder());
    NamedCommands.registerCommand("ShootGamePiece", new ShootGamePiece());

    AutoBuilder.configureHolonomic(
      m_drivetrain::getRobotPose2d,
      m_drivetrain::setRobotPose2d,
      m_drivetrain::getRobotRelativeChassisSpeeds,
      m_drivetrain::driveRobotRelative,
      new HolonomicPathFollowerConfig(
        new PIDConstants(2.5),  // TODO: tune PIDs.
        new PIDConstants(10.0),
        DriveMotor.MAX_SPEED_METERS_PER_SEC,
        Drivetrain.CENTER_TO_WHEEL_OFFSET_METERS,
        new ReplanningConfig()
      ),
      () -> {
        Optional<Alliance> alliance = DriverStation.getAlliance();
        if (alliance.isPresent()) {
          return alliance.get() == DriverStation.Alliance.Red;
        }
        return false;
      },
      m_drivetrain
    );

    autoChooser = AutoBuilder.buildAutoChooser();
    SmartDashboard.putData("Auto chooser", autoChooser);

    // Controls init.
    m_driverController = new CommandXboxController(DRIVER_CONTROLLER_PORT);
    m_operatorController = new CommandXboxController(OPERATOR_CONTROLLER_PORT);

    configureBindings();

    Logger.configureLoggingAndConfig(this, false);
  }

  private void configureBindings() {
    m_drivetrain.setDefaultCommand(
      Commands.run(
        () -> m_drivetrain.drive(
          m_driverController.getLeftX(),
          m_driverController.getLeftY(),
          m_driverController.getRightX()
        ),
        m_drivetrain
      )
    );

    m_driverController.a().onTrue(new InstantCommand(m_drivetrain::resetGyro));

    m_operatorController.a().onTrue(new ScoreAmp());
    m_operatorController.x().onTrue(new PickupPiece());

    m_operatorController.leftTrigger().onTrue(
      new ParallelCommandGroup(
        new AutoAimShoulder(),
        new SetShooterVelocity(Shooter.SHOOTING_SPEED_RPM, false)
      )
    );
    m_operatorController.leftBumper().onTrue(
      new ParallelCommandGroup(
        new SetShoulderPosition(Shoulder.ZERO_POSITION_DEGREES, false),
        new InstantCommand(() -> m_shooter.setPercentOutput(0.0), m_shooter)
      )
    );
  
    m_operatorController.rightTrigger().onTrue(new ShootGamePiece());
  }

  // Use this to pass the autonomous command to the main Robot.java class.
  public Command getAutonomousCommand() {
    return autoChooser.getSelected();
  }
}
