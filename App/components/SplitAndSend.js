import React, { Component } from 'react';
import {
  Platform, StyleSheet, Text, View, Image, Modal, NetInfo,
  ActivityIndicator, TextInput, Picker, AsyncStorage, ToastAndroid
} from 'react-native';
import RNFS from 'react-native-fs';
import ImagePicker from 'react-native-image-picker';
import { Button } from 'react-native-elements'
import { elementsIcon } from 'react-native-elements'
import RNThumbnail from 'react-native-thumbnail';

export default class SplitAndSend extends Component {

  static navigationOptions = ({ navigation }) => {
    return {
      title: 'Send Media',
      headerRight: (<Button title={global.userName.toUpperCase()}
        backgroundColor="green"
        buttonStyle={{ height: 10, marginRight: -15 }}
        fontWeight="bold"
        iconRight={{ name: 'user', type: 'evilicon', size: 25 }}
        onPress={this.profile} />)
    };
  };


  constructor(props) {
    super(props);
    this.state = {
      user: '',
      listOfUsers: [],
      avatarSource: {},
      mediaReady: false,
      imageReady: false,
      mediaResponse: {},
      totalFragments: 0,
      progressbar: false,
      isAlreadyMedidaUploaded: false,
      mediaType: '',
      mediaName: '',
      modalVisible: false,
      splitStatus: true,
      thumbnailPath: '',
      mediaDirPath: '',
      socketState: ''
    };
  }


  componentWillMount = () => {
    const { navigation } = this.props;
    let webSocket = new WebSocket('ws://' + myip);
    webSocket.onopen = () => {
      let request = {
        action: 'getUsers',
        username: global.userName.toLowerCase()
      };
      webSocket.send(JSON.stringify(request));
    };
    webSocket.onmessage = (event) => {
      console.log("AVALABLE USERS ----->  ", event.data);
      this.setState({
        listOfUsers: JSON.parse(event.data)
      });
    }
  }

  sendMedia = async () => {
    if (this.state.user == '') {
      alert("Please select the user...!")
    } else {
      if (this.state.mediaReady == false) {
        alert("Please capture the image or video to send...!");
      } else {
        this.setState({ progressbar: true, modalVisible: true });
        console.log("FILE PATH ==> ", this.state.mediaResponse.path);
        this.getMediaDetails(this.state.mediaName, (result) => {
          console.log(result.data);
          let response = JSON.parse(result.data);
          if (response.length == 0) {
            if (this.state.mediaType == 'video') {
              RNFS.stat(this.state.mediaResponse.uri)
                .then((stat) => {
                  console.log("MY STAT===>", stat);
                  this.splitMedia(stat.originalFilepath, stat.size);
                })
                .catch((err) => {
                  this.setState({ progressbar: false });
                  console.log(err);
                  ToastAndroid.show(err, 500);
                });
            } else {
              this.splitMedia(this.state.mediaResponse.path, this.state.mediaResponse.fileSize);
            }
          } else {
            if (response[0].status == 'done') {
              if (response[0].receiver == this.state.user && response[0].sender == global.userName.toLowerCase()) {
                ToastAndroid.show('Media already sent...!', 1000);
                this.setState({ progressbar: false, modalVisible: false});
              } else {
                console.log('sending same media to other user');
                this.sendSameMediaToOtherUser(response);
              }
            } else { //resend if not send.
              this.resendMedia(response[0].filename);
            }
          }
        });
      }
    }
  };

  sendSameMediaToOtherUser = (response) => {
    let webSocket = new WebSocket('ws://' + myip);
    this.setState({ splitStatus: false });
    webSocket.onopen = () => {
      mediaInfo = {
        action: 'resend',
        receiver: this.state.user,
        mediaInfo : response[0]
      };
      webSocket.send(JSON.stringify(mediaInfo));
    };
    webSocket.onmessage = (event) => {
      this.setState({modalVisible:false, progressbar:false});
      if(event.data == 'true'){
        ToastAndroid.show('Media not Sent to user..!',1000);
      } else {
        ToastAndroid.show('Media not Sent to user..!',1000)
      }
    }
  };

  splitMedia = async (filePath, fileSize) => {
    let chunckSize = 45 * 1000;//45kb
    let totalFragments = Math.ceil(fileSize / chunckSize);
    this.setState({ totalFragments: totalFragments });
    let count = 1;
    let start = 0;
    if (this.state.isAlreadyMedidaUploaded == false) {
      for (let index = 1; index <= totalFragments; index++) {
        this.createFragments(filePath, index, start, chunckSize);
        start = start + chunckSize;
      }
    } else {
      this.setState({ modalVisible: false });
      alert("media alredy sent...!");
    }
  }

  createFragments = async (filePath, count, start, chunckSize) => {
    RNFS.read(filePath, chunckSize, start, 'ascii')
      .then((chunck) => {
        console.log("CHUNCK DATA SIZE ==>::" + count, chunck.length);
        RNFS.writeFile(filePath + "_" + count, chunck, 'ascii')
          .then((success) => {
            console.log("fragment " + filePath + "_" + count + " created..!");
            if (count == this.state.totalFragments) {
              console.log("ALL FRAGMENT CREATED..!");
              this.sendFragments();
            }
          });
      }).catch((err) => {
        console.log("CREATE FRAGMENT ERROR:", err);
      });
  }

  sendFragments = async () => {
    const { navigation } = this.props;
    let webSocket = new WebSocket('ws://' + myip);
    this.setState({ splitStatus: false });
    webSocket.onopen = () => {
      let index = 1;
      global.sentFragmentsInterval = setInterval(() => {
        if (index >= this.state.totalFragments) {
          clearInterval(global.sentFragmentsInterval);
        }
        this.readAndSendFragment(webSocket, index);
        index = index + 1;
      }, 1);
    };

    webSocket.onmessage = (event) => {
      this.finalResponse(event);
    };
    webSocket.onclose = (close) => {
      console.log('socket closed------------>', close);
      clearInterval(global.sentFragmentsInterval);
      this.setState({
        socketState: 'closed',
        progressbar: false,
      });
      ToastAndroid.show('error occurred please retry', ToastAndroid.LONG);
    }
  };

  finalResponse = (event) => {
    let response = JSON.parse(event.data);
    if (response.fragment) {
      console.log('fragment sent ---------->', response.fragment);
    } else {
      let mediaStatus = response.mediaStatus;
      let mediaName = response.fileName;
      if (mediaStatus == 'done') {
        this.setState({
          isAlreadyMedidaUploaded: true,
          progressbar: false,
          modalVisible: false
        });
        ToastAndroid.show(
          "Media is alredy sent to user....!",
          ToastAndroid.LONG
        );
      } else if (mediaStatus == 'error') {
        this.setState({ progressbar: false, modalVisible: false });
        ToastAndroid.show(
          "Error while uploading " + mediaName + " to AWS S3 cloud..!",
          ToastAndroid.LONG
        );
        ToastAndroid.show('Retrying please wait...!', 700);
        // this.sendFragments();
      } else {
        this.setState({ progressbar: false, modalVisible: false });
        ToastAndroid.show(
          'Media ' + response.fileName + ' sent..!',
          ToastAndroid.LONG
        );
        for (let index = 1; index <= this.state.totalFragments; index++) {
          RNFS.unlink(this.state.mediaDirPath + mediaName + "_" + index);
        }
      }
    }
  };

  getMediaDetails = (mediaName, callback) => {
    NetInfo.isConnected.fetch().then(isConnected => {
      if (isConnected == false) {
        ToastAndroid.show('offline connect to network..!', 700);
      } else {
        this.setState({ progressbar: true, socketState: 'connecting' });
        let webSocket = new WebSocket('ws://' + myip);
        webSocket.onopen = () => {
          let request = {
            action: 'getMediaDetails',
            filename: mediaName,
            sender : global.userName.toLowerCase(),
            receiver : this.state.user
          };
          webSocket.send(JSON.stringify(request));
        };
        webSocket.onmessage = (event) => {
          callback(event);
        };
      }
    });
  }

  resendMedia = (mediaName) => {
    this.getMediaDetails(mediaName, (result) => {
      console.log(result.data);
      let response = JSON.parse(result.data);
      if (response.length == 0) {
        console.log('for the first time...............>>!');
        this.sendFragments();
      } else {
        console.log('for the resending time...............>>!');
        this.resendingFragments(result);
      }
    });
  }

  resendingFragments = (event) => {
    let webSocket = new WebSocket('ws://' + myip);
    let response = JSON.parse(event.data);
    let fragments = response[0].fragments;
    webSocket.onopen = () => {
      for (let index = 1; index <= response[0].total_count; index++) {
        if (fragments.includes(index) == false) {
          this.readAndSendFragment(webSocket, index);
        }
      }
    };
    webSocket.onmessage = (event) => {
      this.finalResponse(event);
    };
    webSocket.onclose = (close) => {
      console.log('socket closed------------>', close);
      this.setState({
        socketState: 'closed',
        progressbar: false,
      });
      ToastAndroid.show('error occurred please retry', ToastAndroid.LONG);
    }
  }

  readAndSendFragment = async (webSocket, index) => {
    let totalFragments = this.state.totalFragments;
    let fragmentPath = this.state.mediaResponse.path;
    let fileName = this.state.mediaName;
    let fragmentInfo = {};
    RNFS.readFile(fragmentPath + "_" + index, 'base64')
      .then((content) => {
        fragmentInfo = {
          action: 'merge',
          sender: global.userName,
          receiver: this.state.user,
          fileName: fileName,
          contentType: this.state.mediaType,
          fragmentNumber: index,
          fragmentsCount: totalFragments,
          fragmentContent: content
        };
        console.log("fragment " + fileName + "_" + index + " sent.....!");
        webSocket.send(JSON.stringify(fragmentInfo));
      }).catch((err) => {
        console.log('ERROR IN SEND FRAGMENTS', err);
      });
  }

  openCamera = () => {
    var options = {
      title: 'Select Avatar',
      noData: true,
      customButtons: [
        { name: 'video', title: 'Take Video' },
        { name: 'videoLibrary', title: 'Choose Video from Library' },
      ],
      storageOptions: {
        skipBackup: true,
        path: 'images'
      }
    };

    ImagePicker.showImagePicker(options, (response) => {
      console.log('Response = ', response);
      this.setState({ progressbar: false, splitStatus: true });
      if (response.didCancel) {
        console.log('User cancelled image picker');
      }
      else if (response.error) {
        console.log('ImagePicker Error: ', response.error);
      }
      else if (response.customButton) {
        console.log('User tapped custom button: ', response.customButton);
        var options = {
          noData: true,
          mediaType: 'video',
          storageOptions: {
            skipBackup: true,
            path: 'videos'
          }
        };
        if (response.customButton == 'video') {
          ImagePicker.launchCamera(options, (response) => {
            console.log(response);
            if (response.didCancel) {
              console.log("user clicked cancel..!");
            } else {
              RNThumbnail.get(response.path).then((result) => {
                this.setVideoState(response, result.path);
              })
            }
          });
        } else {
          ImagePicker.launchImageLibrary(options, (response) => {
            console.log(response);
            if (response.didCancel) {
              console.log("user clicked cancel..!");
            } else {
              RNThumbnail.get(response.path).then((result) => {
                this.setVideoState(response, result.path);
              })
            }
          });
        }
      }
      else {
        let mediaDirPath = response.path.replace(response.fileName, '');
        let source = { uri: response.uri };
        this.setState({
          mediaReady: true,
          mediaType: 'image',
          mediaDirPath: mediaDirPath,
          imageReady: true,
          mediaName: response.fileName,
          avatarSource: source,
          mediaResponse: response,
          isAlreadyMedidaUploaded: false,
        });
      }
    });
  }

  setVideoState = (videoResponse, thumPath) => {
    console.log("Inside setVideoState...!");
    let length = videoResponse.path.split("/").length;
    let mediaName = videoResponse.path.split("/")[length - 1];
    let mediaDirPath = videoResponse.path.replace(mediaName, '');

    this.setState({
      thumbnailPath: thumPath,
      mediaName: mediaName,
      mediaDirPath: mediaDirPath,
      mediaReady: true,
      imageReady: false,
      mediaType: 'video',
      mediaResponse: videoResponse,
      isAlreadyMedidaUploaded: false,
    });
  }

  showFragments = () => {
    let fragments = ''
    for (let index = 0; index < this.state.totalFragments; index++) {
      fragments = fragments + '[' + index + ']' + this.state.mediaResponse.path + "\n";
    }
    if (this.state.totalFragments > 0)
      alert(fragments);
    else {
      alert("fragments are not generated...!")
    }
  }

  render() {
    return (
      <View style={styles.container} >
        <View>
          <Modal
            animationType="slide"
            transparent={true}
            onRequestClose={() => { this.setState({ modalVisible: false }) }}
            visible={this.state.modalVisible}>
            <View style={styles.modalContainer}>
              <ActivityIndicator size="large" color="#ffffff"
                animating={this.state.progressbar} />
              {
                (this.state.splitStatus) ?
                  <Text style={styles.text}>Please wait Spliting Media..!</Text>
                  : <Text style={styles.text}>Please wait Sending Media...!</Text>
              }
              {
                (this.state.socketState != 'closed') ?
                  <Button title='close' rounded
                    onPress={() => { this.setState({ modalVisible: false }) }}
                  /> :
                  <Button title='retry' rounded
                    onPress={() => { this.resendMedia(this.state.mediaName) }}
                  />
              }
            </View>
          </Modal>
        </View>
        <View style={styles.picker}>
          <Picker
            selectedValue={this.state.user}
            style={{ height: 50, width: 200 }}
            onValueChange={(itemValue, itemIndex) => this.setState({ user: itemValue })}>
            <Picker.Item value="" label="List of Users" />
            <Picker.Item value="guest" label="guest" />
            <Picker.Item value="pandit" label="pandit" />
            {
              Object.values(this.state.listOfUsers).map((values) => {
                return (
                  <Picker.Item key={values} value={values} label={values} />
                );
              })
            }
          </Picker>
        </View>
        <View style={{ marginBottom: 10 }}>
          <Button rounded
            title="Select Media"
            iconRight={{ name: 'upload', type: 'feather' }}
            buttonStyle={styles.buttons}
            color="#fff" onPress={this.openCamera} />

          <Button title='Send' rounded
            iconRight={{ name: 'send', type: 'ionicons' }}
            buttonStyle={styles.buttons}
            color="#fff" onPress={this.sendMedia}
          />
        </View>
        {(this.state.imageReady && this.state.mediaType == 'image')
          ? <Image source={this.state.avatarSource} style={styles.imageStyle} />
          : <Image source={{ uri: this.state.thumbnailPath }} style={styles.imageStyle} />
        }
        <View>
          <Text>{this.state.mediaName}</Text>
        </View>
      </View >
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  modalContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(52, 52, 52, 0.8)'
  },
  buttons: {
    marginTop: 20,
    width: 200,
    height: 35,
    backgroundColor: '#123456',
  },
  imageStyle: {
    justifyContent: 'center',
    alignItems: 'center',
    width: 200,
    height: 200,
    marginTop: 10,
    borderColor: 'green',
    borderWidth: 5,
    borderRadius: 10,
  },
  picker: {
    borderWidth: 2,
    borderRadius: 7
  },
  text: {
    fontWeight: 'bold',
    color: 'white',
    fontSize: 18
  }
});
