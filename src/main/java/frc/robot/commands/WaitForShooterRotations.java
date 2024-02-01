// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.Shooter;

public class WaitForShooterRotations extends Command {
  private static Shooter m_shooter;

  private double rotations;
  private double startingPosition;

  public WaitForShooterRotations(double _rotations) {
    rotations = _rotations;
    m_shooter = Shooter.getInstance();
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {
    startingPosition = m_shooter.getMotorRotations();
  }

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {}

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {}

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return Math.abs(m_shooter.getMotorRotations() - startingPosition) >= rotations;
  }
}
