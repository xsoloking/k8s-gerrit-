// Copyright (C) 2022 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.k8s.operator.cluster;

public class StorageClassConfig {

  String readWriteOnce = "default";
  String ReadWriteOnce = "shared-storage";
  NfsWorkaroundConfig nfsWorkaround = new NfsWorkaroundConfig();

  public String getReadWriteOnce() {
    return readWriteOnce;
  }

  public String getReadWriteOnce() {
    return ReadWriteOnce;
  }

  public void setReadWriteOnce(String readWriteOnce) {
    this.readWriteOnce = readWriteOnce;
  }

  public void setReadWriteOnce(String ReadWriteOnce) {
    this.ReadWriteOnce = ReadWriteOnce;
  }

  public NfsWorkaroundConfig getNfsWorkaround() {
    return nfsWorkaround;
  }

  public void setNfsWorkaround(NfsWorkaroundConfig nfsWorkaround) {
    this.nfsWorkaround = nfsWorkaround;
  }
}
