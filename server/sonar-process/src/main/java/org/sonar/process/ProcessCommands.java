/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.process;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Process inter-communication to :
 * <ul>
 *   <li>share status of child process</li>
 *   <li>stop child process</li>
 * </ul>
 *
 * <p/>
 * It relies on files shared by both processes. Following alternatives were considered but not selected :
 * <ul>
 *   <li>JMX beans over RMI: network issues (mostly because of Java reverse-DNS) + requires to configure and open a new port</li>
 *   <li>simple socket protocol: same drawbacks are RMI connection</li>
 *   <li>java.lang.Process#destroy(): shutdown hooks are not executed on some OS (mostly MSWindows)</li>
 *   <li>execute OS-specific commands (for instance kill on *nix): OS-specific, so hell to support. Moreover how to get identify a process ?</li>
 * </ul>
 */
public class ProcessCommands {

  private final RandomAccessFile sharedmemory; // We need to keep a reference to this variable to prevent it to be garbage collected
  /**
   * The ByteBuffer will contains :
   * <ul>
   *   <li>First byte will contains 0x00 until stop command is issued = 0xFF</li>
   *   <li>Then each 9 bytes will contains first the status of process (0x01 : READY) then a long (8 bytes) with the latest ping</li>
   * </ul>
   */
  final MappedByteBuffer mappedByteBuffer;
  private static final int MAX_PROCESSES = 50;
  private static final int MAX_SHARED_MEMORY = 1 + 9 * MAX_PROCESSES; // With this shared memory we can handle up to MAX_PROCESSES processes
  public static final byte STOP = (byte) 0xFF;
  public static final byte READY = (byte) 0x01;
  public static final byte EMPTY = (byte) 0x00;

  private int processNumber;

  public ProcessCommands(File directory, int processNumber) {
    // processNumber should not excess MAX_PROCESSES and must not be below -1
    assert processNumber <= MAX_PROCESSES : "Incorrect process number";
    assert processNumber >= -1 : "Incorrect process number";

    this.processNumber = processNumber;
    if (!directory.isDirectory() || !directory.exists()) {
      throw new IllegalArgumentException("Not a valid directory: " + directory);
    }

    try {
      sharedmemory = new RandomAccessFile(new File(directory, "sharedmemory"), "rw");
      mappedByteBuffer = sharedmemory.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, MAX_SHARED_MEMORY);
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to create shared memory : ", e);
    }
  }

  public boolean isReady() {
    return canBeMonitored() && mappedByteBuffer.get(offset()) == READY;
  }

  /**
   * To be executed by child process to declare that it's ready
   */
  public void setReady() {
    if (canBeMonitored()) {
      mappedByteBuffer.put(offset(), READY);
    }
  }

  public void ping() {
    if (canBeMonitored()) {
      mappedByteBuffer.putLong(1 + offset(), System.currentTimeMillis());
    }
  }

  public long getLastPing() {
    if (canBeMonitored()) {
      return mappedByteBuffer.getLong(1 + offset());
    } else {
      return -1;
    }
  }

  /**
   * To be executed by monitor process to ask for child process termination
   */
  public void askForStop() {
    mappedByteBuffer.put(0, STOP);
  }

  public boolean askedForStop() {
    return mappedByteBuffer.get(0) == STOP;
  }

  public int offset() {
    return 1 + 9 * processNumber;
  }

  private boolean canBeMonitored() {
    boolean result = processNumber >= 0 && processNumber < MAX_PROCESSES;
    if (!result) {
      LoggerFactory.getLogger(getClass()).info("This process cannot be monitored. Process Id : [{}]", processNumber);
    }
    return result;
  }

  public static final int getMaxProcesses() {
    return MAX_PROCESSES;
  }
}